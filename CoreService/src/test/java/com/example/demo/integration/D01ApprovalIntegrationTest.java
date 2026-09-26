package com.example.demo.integration;

import com.example.demo.exception.ForbiddenException;
import com.example.demo.exception.NotFoundException;
import com.example.demo.integration.support.MySqlTestDatabase;
import com.example.demo.mapper.ChatRoomMapper;
import com.example.demo.mapper.Market.ProductImageMapper;
import com.example.demo.mapper.Market.ProductMapper;
import com.example.demo.mapper.Market.ProductRequestMapper;
import com.example.demo.mapper.Market.TransactionsMapper;
import com.example.demo.mapper.Market.UserLocationMapper;
import com.example.demo.service.ChatService;
import com.example.demo.service.NotificationService;
import com.example.demo.service.Market.ImageUploadService;
import com.example.demo.service.Market.ProductService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@Tag("mysql")
class D01ApprovalIntegrationTest {
    private static final String OWNER = "host@r03.example.test";
    private static final String REQUESTER = "member@r03.example.test";
    private static final String OTHER = "outsider@r03.example.test";

    private MySqlTestDatabase database;
    private NotificationService notifications;
    private ProductService service;

    @BeforeEach
    void setUp() throws Exception {
        database = MySqlTestDatabase.create();
        database.seedFixture();
        notifications = mock(NotificationService.class);
        service = service(database.sqlSession().getMapper(TransactionsMapper.class), notifications);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (database != null) database.close();
    }

    @Test
    void approvalCommitsRequestParticipantVisibilityTransactionThenNotificationOnce() {
        product(101, 1);
        request(201, 101, REQUESTER);

        service.approveProductRequest(OWNER, 101L, 201L);

        assertEquals("승인", value("SELECT approval_status FROM productrequests WHERE id=201", String.class));
        assertEquals("완료", value("SELECT status FROM productrequests WHERE id=201", String.class));
        assertEquals(1, value("SELECT current_participants FROM Products WHERE id=101", Integer.class));
        assertFalse(value("SELECT is_visible FROM Products WHERE id=101", Boolean.class));
        assertEquals(1, count("SELECT COUNT(*) FROM Transactions WHERE product_id=101 AND buyer_email=? AND seller_email=?", REQUESTER, OWNER));
        verify(notifications, times(1)).sendNotification(REQUESTER, "\"D01 product 101\" 요청이 승인되었습니다!", "PRODUCT_REQUEST", 0, 101L);
    }

    @Test
    void directApprovalAppliesOwnerBeforeRequestAndHidesMismatchedRequest() {
        product(101, 2);
        product(102, 2);
        request(201, 102, REQUESTER);
        request(202, 101, OWNER);

        assertThrows(ForbiddenException.class, () -> service.approveProductRequest(OTHER, 101L, 999L));
        assertThrows(NotFoundException.class, () -> service.approveProductRequest(OWNER, 101L, 999L));
        assertThrows(NotFoundException.class, () -> service.approveProductRequest(OWNER, 101L, 201L));
        assertThrows(ForbiddenException.class, () -> service.approveProductRequest(OWNER, 101L, 202L));

        assertEquals(0, approved(101));
        assertEquals(0, approved(102));
        assertEquals(0, count("SELECT COUNT(*) FROM Transactions WHERE product_id IN (101, 102)"));
        verify(notifications, never()).sendNotification(any(), any(), any(), any(), any());
    }

    @Test
    void chatroomApprovalUsesTheSameApprovalCore() {
        product(101, 1);
        request(201, 101, REQUESTER);
        chatroom(301, 101, REQUESTER);

        service.approveProductRequestByChatroom(OWNER, 301);

        assertEquals(1, approved(101));
        assertEquals(1, value("SELECT current_participants FROM Products WHERE id=101", Integer.class));
        assertEquals(1, count("SELECT COUNT(*) FROM Transactions WHERE product_id=101"));
        verify(notifications, times(1)).sendNotification(any(), any(), any(), any(), any());
    }

    @Test
    void chatroomApprovalHidesOutsiderRejectsRequesterAndValidatesRequestRelationship() {
        product(101, 2);
        request(201, 101, REQUESTER);
        chatroom(301, 101, REQUESTER);
        chatroom(302, 101, OTHER);

        assertThrows(NotFoundException.class,
                () -> service.approveProductRequestByChatroom(OTHER, 301));
        assertThrows(ForbiddenException.class,
                () -> service.approveProductRequestByChatroom(REQUESTER, 301));
        assertThrows(NotFoundException.class,
                () -> service.approveProductRequestByChatroom(OWNER, 302));

        assertEquals(0, approved(101));
        assertEquals(0, count("SELECT COUNT(*) FROM Transactions WHERE product_id=101"));
        verify(notifications, never()).sendNotification(any(), any(), any(), any(), any());
    }

    @Test
    void repeatedRequestAndDuplicateRequesterCreateOneApprovalAndTransaction() {
        product(101, 3);
        request(201, 101, REQUESTER);
        request(202, 101, REQUESTER);

        service.approveProductRequest(OWNER, 101L, 201L);
        service.approveProductRequest(OWNER, 101L, 201L);
        service.approveProductRequest(OWNER, 101L, 202L);

        assertEquals(1, approved(101));
        assertEquals("미승인", value("SELECT approval_status FROM productrequests WHERE id=202", String.class));
        assertEquals(1, value("SELECT current_participants FROM Products WHERE id=101", Integer.class));
        assertEquals(1, count("SELECT COUNT(*) FROM Transactions WHERE product_id=101"));
        verify(notifications, times(1)).sendNotification(any(), any(), any(), any(), any());
    }

    @Test
    void concurrentLastSeatApprovesExactlyOneRequest() throws Exception {
        product(101, 1);
        request(201, 101, REQUESTER);
        request(202, 101, OTHER);
        CountDownLatch start = new CountDownLatch(1);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            List<Future<Boolean>> results = List.of(
                    executor.submit(() -> approveAfter(start, 201)),
                    executor.submit(() -> approveAfter(start, 202)));
            start.countDown();

            assertEquals(1, results.stream().filter(this::succeeded).count());
        } finally {
            executor.shutdownNow();
        }

        assertEquals(1, approved(101));
        assertEquals(1, value("SELECT current_participants FROM Products WHERE id=101", Integer.class));
        assertEquals(1, count("SELECT COUNT(*) FROM Transactions WHERE product_id=101"));
        verify(notifications, times(1)).sendNotification(any(), any(), any(), any(), any());
    }

    @Test
    void transactionInsertFailureRollsBackApprovalAndSkipsNotification() {
        product(101, 1);
        request(201, 101, REQUESTER);
        TransactionsMapper failingTransactions = mock(TransactionsMapper.class);
        doThrow(new IllegalStateException("forced transaction insert failure"))
                .when(failingTransactions).insertTransaction(any());
        ProductService failingService = service(failingTransactions, notifications);

        assertThrows(IllegalStateException.class,
                () -> failingService.approveProductRequest(OWNER, 101L, 201L));

        assertEquals("미승인", value("SELECT approval_status FROM productrequests WHERE id=201", String.class));
        assertEquals("대기", value("SELECT status FROM productrequests WHERE id=201", String.class));
        assertEquals(0, value("SELECT current_participants FROM Products WHERE id=101", Integer.class));
        assertEquals(0, count("SELECT COUNT(*) FROM Transactions WHERE product_id=101"));
        verify(notifications, never()).sendNotification(any(), any(), any(), any(), any());
    }

    @Test
    void notificationFailureAfterCommitDoesNotUndoApproval() {
        product(101, 1);
        request(201, 101, REQUESTER);
        doThrow(new IllegalStateException("forced notification failure"))
                .when(notifications).sendNotification(any(), any(), any(), any(), any());

        assertDoesNotThrow(() -> service.approveProductRequest(OWNER, 101L, 201L));

        assertEquals(1, approved(101));
        assertEquals(1, count("SELECT COUNT(*) FROM Transactions WHERE product_id=101"));
        verify(notifications, times(1)).sendNotification(any(), any(), any(), any(), any());
    }

    private ProductService service(TransactionsMapper transactions, NotificationService notificationService) {
        ProductService target = new ProductService(
                database.sqlSession().getMapper(ProductMapper.class),
                database.sqlSession().getMapper(ProductRequestMapper.class),
                database.sqlSession().getMapper(ChatRoomMapper.class),
                mock(ProductImageMapper.class), notificationService, mock(ImageUploadService.class),
                mock(ChatService.class), transactions, mock(UserLocationMapper.class));
        TransactionInterceptor interceptor = new TransactionInterceptor(
                new DataSourceTransactionManager(database.dataSource()),
                new AnnotationTransactionAttributeSource());
        ProxyFactory proxy = new ProxyFactory(target);
        proxy.addAdvice(interceptor);
        return (ProductService) proxy.getProxy();
    }

    private boolean approveAfter(CountDownLatch start, long requestId) throws InterruptedException {
        start.await();
        try {
            service.approveProductRequest(OWNER, 101L, requestId);
            return true;
        } catch (IllegalArgumentException expectedWhenFull) {
            return false;
        }
    }

    private boolean succeeded(Future<Boolean> result) {
        try {
            return result.get();
        } catch (Exception exception) {
            throw new AssertionError(exception);
        }
    }

    private void product(long id, int maximum) {
        database.jdbc().update("""
                INSERT INTO Products
                    (id, product_code, title, description, price, email, category_id, hobby_id,
                     transaction_type, registration_type, max_participants, current_participants, days)
                VALUES (?, ?, ?, 'D01 fixture', 1000, ?, 10, 20, '대면', '판매', ?, 0, 'MON')
                """, id, "d01-product-" + id, "D01 product " + id, OWNER, maximum);
    }

    private void request(long id, long productId, String requester) {
        database.jdbc().update(
                "INSERT INTO productrequests (id, product_id, requester_email) VALUES (?, ?, ?)",
                id, productId, requester);
    }

    private void chatroom(int id, long productId, String requester) {
        database.jdbc().update(
                "INSERT INTO chatrooms (chatroom_id, chatname, product_id, request_email) VALUES (?, 'D01 chat', ?, ?)",
                id, productId, requester);
    }

    private int approved(long productId) {
        return count("SELECT COUNT(*) FROM productrequests WHERE product_id=? AND approval_status='승인'", productId);
    }

    private int count(String sql, Object... arguments) {
        return database.jdbc().queryForObject(sql, Integer.class, arguments);
    }

    private <T> T value(String sql, Class<T> type) {
        return database.jdbc().queryForObject(sql, type);
    }
}
