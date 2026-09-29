package com.example.demo.integration;

import com.example.demo.dto.Market.PaymentsRequest;
import com.example.demo.dto.Market.PaymentsResponse;
import com.example.demo.dto.Market.TransactionsRequest;
import com.example.demo.dto.Market.TransactionsResponse;
import com.example.demo.exception.ForbiddenException;
import com.example.demo.exception.NotFoundException;
import com.example.demo.integration.support.MySqlTestDatabase;
import com.example.demo.mapper.ChatRoomMapper;
import com.example.demo.mapper.Market.PaymentsMapper;
import com.example.demo.mapper.Market.ProductImageMapper;
import com.example.demo.mapper.Market.ProductMapper;
import com.example.demo.mapper.Market.ProductRequestMapper;
import com.example.demo.mapper.Market.TransactionsMapper;
import com.example.demo.mapper.Market.UserLocationMapper;
import com.example.demo.service.ChatService;
import com.example.demo.service.NotificationService;
import com.example.demo.service.Market.ImageUploadService;
import com.example.demo.service.Market.PaymentsService;
import com.example.demo.service.Market.ProductService;
import com.example.demo.service.Market.TransactionsService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.AdditionalAnswers;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

@Tag("mysql")
class D04TransactionPaymentIntegrationTest {
    private static final String OWNER = "host@r03.example.test";
    private static final String REQUESTER = "member@r03.example.test";
    private static final String OUTSIDER = "outsider@r03.example.test";

    private MySqlTestDatabase database;
    private TransactionsMapper transactionsMapper;
    private PaymentsMapper paymentsMapper;
    private TransactionsService transactionsService;
    private PaymentsService paymentsService;

    @BeforeEach
    void setUp() throws Exception {
        database = MySqlTestDatabase.create();
        database.seedFixture();
        database.jdbc().update(
                "UPDATE productrequests SET status='완료', approval_status='승인' WHERE id=200");
        transactionsMapper = database.sqlSession().getMapper(TransactionsMapper.class);
        paymentsMapper = database.sqlSession().getMapper(PaymentsMapper.class);
        transactionsService = transactional(new TransactionsService(
                transactionsMapper,
                database.sqlSession().getMapper(ProductMapper.class),
                database.sqlSession().getMapper(ProductRequestMapper.class),
                paymentsMapper));
        paymentsService = transactional(new PaymentsService(paymentsMapper, transactionsMapper));
    }

    @AfterEach
    void tearDown() throws Exception {
        if (database != null) database.close();
    }

    @Test
    void approvedRequestUsesServerFieldsReusesTransactionAndHidesOutsiders() {
        TransactionsRequest untrusted = TransactionsRequest.builder()
                .productId(100L)
                .requestId(200L)
                .buyerEmail(OUTSIDER)
                .sellerEmail(OUTSIDER)
                .price(1)
                .description("client value")
                .build();

        assertThrows(ForbiddenException.class,
                () -> transactionsService.createTransaction(untrusted, OUTSIDER));
        TransactionsResponse created = transactionsService.createTransaction(untrusted, OWNER);
        TransactionsResponse repeated = transactionsService.createTransaction(untrusted, OWNER);

        assertAll(
                () -> assertEquals(created.getId(), repeated.getId()),
                () -> assertEquals(OWNER, created.getBuyerEmail()),
                () -> assertEquals(REQUESTER, created.getSellerEmail()),
                () -> assertEquals(1_000, created.getPrice()),
                () -> assertEquals("진행중", created.getTransactionStatus()),
                () -> assertEquals("미완료", created.getPaymentStatus()),
                () -> assertEquals(1, count("SELECT COUNT(*) FROM Transactions WHERE product_id=100")),
                () -> assertEquals(created.getId(), transactionsService
                        .getTransactionById(created.getId(), REQUESTER).getBody().getData().getId()),
                () -> assertEquals(List.of(created.getId()), transactionsService
                        .getUserTransactions(OWNER).getBody().getData().stream()
                        .map(TransactionsResponse::getId).toList())
        );

        NotFoundException outsider = assertThrows(NotFoundException.class,
                () -> transactionsService.getTransactionById(created.getId(), OUTSIDER));
        NotFoundException missing = assertThrows(NotFoundException.class,
                () -> transactionsService.getTransactionById(999_999L, OUTSIDER));
        assertEquals(missing.getMessage(), outsider.getMessage());
    }

    @Test
    void d01ApprovalTransactionIsReusedThenPaidToCompletion() {
        database.jdbc().update(
                "UPDATE productrequests SET status='대기', approval_status='미승인' WHERE id=200");

        productService().approveProductRequest(OWNER, 100L, 200L);
        List<TransactionsResponse> approvedTransactions = transactionsMapper.findTransactionsByUser(OWNER);
        assertEquals(1, approvedTransactions.size());
        TransactionsResponse approved = approvedTransactions.get(0);

        TransactionsResponse reused = transactionsService.createTransaction(
                TransactionsRequest.builder().productId(100L).requestId(200L)
                        .buyerEmail(OUTSIDER).sellerEmail(OUTSIDER).price(1).build(), OWNER);
        PaymentsResponse payment = paymentsService.createPayment(PaymentsRequest.builder()
                .transactionId(approved.getId()).amount(1).paymentMethod("카드").build(), OWNER)
                .getBody().getData();

        assertAll(
                () -> assertEquals(approved.getId(), reused.getId()),
                () -> assertNotNull(payment.getId()),
                () -> assertEquals(approved.getId(), payment.getTransactionId()),
                () -> assertEquals(1_000, payment.getAmount()),
                () -> assertEquals(1, count("SELECT COUNT(*) FROM Transactions WHERE product_id=100")),
                () -> assertEquals(1, count("SELECT COUNT(*) FROM Payments WHERE transaction_id=?", approved.getId())),
                () -> assertEquals(payment.getId(), value(
                        "SELECT id FROM Payments WHERE transaction_id=?", Long.class, approved.getId())),
                () -> assertEquals("완료", value(
                        "SELECT transaction_status FROM Transactions WHERE id=?", String.class, approved.getId())),
                () -> assertEquals("완료", value(
                        "SELECT payment_status FROM Transactions WHERE id=?", String.class, approved.getId()))
        );
    }

    @Test
    void concurrentCreateReusesOneTransaction() throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            List<Future<Long>> results = List.of(
                    executor.submit(() -> createAfter(start)),
                    executor.submit(() -> createAfter(start)));
            start.countDown();
            assertEquals(results.get(0).get(), results.get(1).get());
        } finally {
            executor.shutdownNow();
        }
        assertEquals(1, count("SELECT COUNT(*) FROM Transactions WHERE product_id=100"));
    }

    @Test
    void mismatchedAndUnapprovedRequestsDoNotCreateTransaction() {
        database.jdbc().update("""
                INSERT INTO Products
                    (id, product_code, title, description, price, email, category_id, hobby_id,
                     transaction_type, registration_type, max_participants, current_participants, days)
                VALUES (101, 'd04-product-101', 'D04 product', 'fixture', 2000, ?, 10, 20,
                        '대면', '판매', 1, 0, 'MON')
                """, OWNER);
        database.jdbc().update(
                "INSERT INTO productrequests (id, product_id, requester_email) VALUES (201, 101, ?)", REQUESTER);

        assertThrows(NotFoundException.class, () -> transactionsService.createTransaction(
                TransactionsRequest.builder().productId(100L).requestId(201L).build(), OWNER));
        assertThrows(NotFoundException.class, () -> transactionsService.createTransaction(
                TransactionsRequest.builder().productId(101L).requestId(201L).build(), OWNER));
        assertEquals(0, count("SELECT COUNT(*) FROM Transactions"));
    }

    @Test
    void buyerPaysServerCalculatedBalanceAndReceivesNewPaymentId() {
        TransactionsResponse transaction = createTransaction();
        PaymentsRequest invalid = PaymentsRequest.builder()
                .transactionId(transaction.getId()).amount(1).build();
        assertThrows(IllegalArgumentException.class,
                () -> paymentsService.createPayment(invalid, OWNER));
        assertThrows(NotFoundException.class,
                () -> paymentsService.createPayment(PaymentsRequest.builder()
                        .transactionId(transaction.getId()).amount(1).paymentMethod("카드").build(), OUTSIDER));
        assertThrows(ForbiddenException.class,
                () -> paymentsService.createPayment(PaymentsRequest.builder()
                        .transactionId(transaction.getId()).amount(1).paymentMethod("카드").build(), REQUESTER));
        PaymentsRequest earlier = PaymentsRequest.builder()
                .transactionId(transaction.getId()).amount(200).paymentMethod("포인트").build();
        paymentsMapper.insertPayment(earlier);

        PaymentsRequest request = PaymentsRequest.builder()
                .transactionId(transaction.getId()).amount(99_999).paymentMethod("카드").build();
        PaymentsResponse created = paymentsService.createPayment(request, OWNER).getBody().getData();

        assertAll(
                () -> assertNotEquals(earlier.getId(), created.getId()),
                () -> assertEquals(request.getId(), created.getId()),
                () -> assertEquals(800, created.getAmount()),
                () -> assertEquals("완료", value("SELECT transaction_status FROM Transactions WHERE id=?", String.class, transaction.getId())),
                () -> assertEquals("완료", value("SELECT payment_status FROM Transactions WHERE id=?", String.class, transaction.getId())),
                () -> assertEquals(2, paymentsService.getPaymentsByTransaction(
                        transaction.getId(), REQUESTER).getBody().getData().size())
        );
        assertThrows(IllegalArgumentException.class,
                () -> paymentsService.createPayment(request, OWNER));
        NotFoundException missing = assertThrows(NotFoundException.class,
                () -> paymentsService.getPaymentsByTransaction(999_999L, OUTSIDER));
        NotFoundException outsider = assertThrows(NotFoundException.class,
                () -> paymentsService.getPaymentsByTransaction(transaction.getId(), OUTSIDER));
        assertEquals(missing.getMessage(), outsider.getMessage());
    }

    @Test
    void memberCanCancelOnlyBeforePaymentAndCancelledTransactionCannotBePaid() {
        TransactionsResponse transaction = createTransaction();

        assertThrows(NotFoundException.class,
                () -> transactionsService.cancelTransaction(transaction.getId(), OUTSIDER));
        TransactionsResponse cancelled = transactionsService
                .cancelTransaction(transaction.getId(), REQUESTER).getBody().getData();

        assertEquals("취소", cancelled.getTransactionStatus());
        assertThrows(IllegalArgumentException.class,
                () -> transactionsService.cancelTransaction(transaction.getId(), OWNER));
        assertThrows(IllegalArgumentException.class, () -> paymentsService.createPayment(
                PaymentsRequest.builder().transactionId(transaction.getId())
                        .amount(1).paymentMethod("카드").build(), OWNER));
    }

    @Test
    void concurrentPaymentCreatesOnePaymentAndCompletesOnce() throws Exception {
        TransactionsResponse transaction = createTransaction();
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            List<Future<Boolean>> results = List.of(
                    executor.submit(() -> payAfter(start, transaction.getId(), "카드")),
                    executor.submit(() -> payAfter(start, transaction.getId(), "계좌이체")));
            start.countDown();
            assertEquals(1, results.stream().filter(this::succeeded).count());
        } finally {
            executor.shutdownNow();
        }

        assertEquals(1, count("SELECT COUNT(*) FROM Payments WHERE transaction_id=?", transaction.getId()));
        assertEquals(1_000L, paymentsMapper.getTotalPaidByTransaction(transaction.getId()));
        assertEquals("완료", value("SELECT transaction_status FROM Transactions WHERE id=?", String.class, transaction.getId()));
    }

    @Test
    void paymentAndCancelRaceAllowsOneStateTransition() throws Exception {
        TransactionsResponse transaction = createTransaction();
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            List<Future<Boolean>> results = List.of(
                    executor.submit(() -> payAfter(start, transaction.getId(), "카드")),
                    executor.submit(() -> cancelAfter(start, transaction.getId())));
            start.countDown();
            assertEquals(1, results.stream().filter(this::succeeded).count());
        } finally {
            executor.shutdownNow();
        }

        String status = value(
                "SELECT transaction_status FROM Transactions WHERE id=?", String.class, transaction.getId());
        int payments = count("SELECT COUNT(*) FROM Payments WHERE transaction_id=?", transaction.getId());
        assertTrue(("완료".equals(status) && payments == 1) || ("취소".equals(status) && payments == 0));
    }

    @Test
    void completionFailureRollsBackInsertedPayment() {
        TransactionsResponse transaction = createTransaction();
        TransactionsMapper failing = mock(
                TransactionsMapper.class, AdditionalAnswers.delegatesTo(transactionsMapper));
        doReturn(0).when(failing).completeTransaction(transaction.getId());
        PaymentsService failingService = transactional(new PaymentsService(paymentsMapper, failing));

        assertThrows(IllegalStateException.class, () -> failingService.createPayment(
                PaymentsRequest.builder().transactionId(transaction.getId())
                        .amount(1).paymentMethod("카드").build(), OWNER));

        assertEquals(0, count("SELECT COUNT(*) FROM Payments WHERE transaction_id=?", transaction.getId()));
        assertEquals("진행중", value("SELECT transaction_status FROM Transactions WHERE id=?", String.class, transaction.getId()));
    }

    private TransactionsResponse createTransaction() {
        return transactionsService.createTransaction(TransactionsRequest.builder()
                .productId(100L).requestId(200L).build(), OWNER);
    }

    private ProductService productService() {
        return transactional(new ProductService(
                database.sqlSession().getMapper(ProductMapper.class),
                database.sqlSession().getMapper(ProductRequestMapper.class),
                database.sqlSession().getMapper(ChatRoomMapper.class),
                mock(ProductImageMapper.class), mock(NotificationService.class),
                mock(ImageUploadService.class), mock(ChatService.class), transactionsMapper,
                mock(UserLocationMapper.class)));
    }

    private Long createAfter(CountDownLatch start) throws InterruptedException {
        start.await();
        return createTransaction().getId();
    }

    private boolean payAfter(CountDownLatch start, long transactionId, String method)
            throws InterruptedException {
        start.await();
        try {
            paymentsService.createPayment(PaymentsRequest.builder()
                    .transactionId(transactionId).amount(1).paymentMethod(method).build(), OWNER);
            return true;
        } catch (IllegalArgumentException expectedDuplicate) {
            return false;
        }
    }

    private boolean cancelAfter(CountDownLatch start, long transactionId) throws InterruptedException {
        start.await();
        try {
            transactionsService.cancelTransaction(transactionId, REQUESTER);
            return true;
        } catch (IllegalArgumentException expectedRaceLoss) {
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

    @SuppressWarnings("unchecked")
    private <T> T transactional(T target) {
        TransactionInterceptor interceptor = new TransactionInterceptor(
                new DataSourceTransactionManager(database.dataSource()),
                new AnnotationTransactionAttributeSource());
        ProxyFactory proxy = new ProxyFactory(target);
        proxy.addAdvice(interceptor);
        return (T) proxy.getProxy();
    }

    private int count(String sql, Object... arguments) {
        return database.jdbc().queryForObject(sql, Integer.class, arguments);
    }

    private <T> T value(String sql, Class<T> type, Object... arguments) {
        return database.jdbc().queryForObject(sql, type, arguments);
    }
}
