export interface IProduct {
  id: number;
  title: string;
  description: string;
  image: string;
  price: number;
  category: string;
  dopamine: number;
  currentParticipants: number;
  maxParticipants: number;
  location: string;
  createdAt: string;
}

// 최신 상품 목록
export const mockLatestProducts: IProduct[] = [
  {
    id: 1,
    title: "주말 테니스 모임",
    description: "초보자도 환영하는 테니스 모임입니다",
    image: "https://picsum.photos/600/400?random=1",
    price: 30000,
    category: "스포츠",
    dopamine: 85,
    currentParticipants: 3,
    maxParticipants: 6,
    location: "서울시 강남구",
    createdAt: "2024-03-15"
  },
  {
    id: 2,
    title: "프로그래밍 스터디",
    description: "주 2회 코딩 스터디",
    image: "https://picsum.photos/600/400?random=2",
    price: 20000,
    category: "DIY",
    dopamine: 75,
    currentParticipants: 4,
    maxParticipants: 8,
    location: "서울시 서초구",
    createdAt: "2024-03-14"
  },
  {
    id: 3,
    title: "주말 등산 모임",
    description: "북한산 등산 함께해요",
    image: "https://picsum.photos/600/400?random=3",
    price: 15000,
    category: "여행",
    dopamine: 90,
    currentParticipants: 5,
    maxParticipants: 10,
    location: "서울시 은평구",
    createdAt: "2024-03-13"
  },
  {
    id: 4,
    title: "베이킹 클래스",
    description: "마카롱 만들기 클래스",
    image: "https://picsum.photos/600/400?random=4",
    price: 40000,
    category: "요리",
    dopamine: 80,
    currentParticipants: 6,
    maxParticipants: 8,
    location: "서울시 마포구",
    createdAt: "2024-03-12"
  },
  {
    id: 9,
    title: "주말 클라이밍",
    description: "실내 클라이밍 초보자 환영",
    image: "https://picsum.photos/600/400?random=9",
    price: 25000,
    category: "스포츠",
    dopamine: 88,
    currentParticipants: 4,
    maxParticipants: 8,
    location: "서울시 강남구",
    createdAt: "2024-03-16"
  },
  {
    id: 10,
    title: "캘리그라피 모임",
    description: "함께 배우는 캘리그라피",
    image: "https://picsum.photos/600/400?random=10",
    price: 30000,
    category: "예술",
    dopamine: 78,
    currentParticipants: 5,
    maxParticipants: 10,
    location: "서울시 마포구",
    createdAt: "2024-03-17"
  },
  {
    id: 11,
    title: "드럼 레슨",
    description: "기초부터 배우는 드럼",
    image: "https://picsum.photos/600/400?random=11",
    price: 45000,
    category: "음악",
    dopamine: 92,
    currentParticipants: 2,
    maxParticipants: 4,
    location: "서울시 서초구",
    createdAt: "2024-03-18"
  },
  {
    id: 12,
    title: "가드닝 클래스",
    description: "다육이 키우기 원데이 클래스",
    image: "https://picsum.photos/600/400?random=12",
    price: 35000,
    category: "과학",
    dopamine: 75,
    currentParticipants: 6,
    maxParticipants: 12,
    location: "서울시 강동구",
    createdAt: "2024-03-19"
  }
];

// 추천 상품 목록
export const mockRecommendedProducts: IProduct[] = [
  {
    id: 5,
    title: "아크릴 페인팅",
    description: "함께하는 아크릴 그림 그리기",
    image: "https://picsum.photos/600/400?random=5",
    price: 35000,
    category: "예술",
    dopamine: 88,
    currentParticipants: 4,
    maxParticipants: 8,
    location: "서울시 종로구",
    createdAt: "2024-03-11"
  },
  {
    id: 6,
    title: "기타 연주 모임",
    description: "초보자를 위한 기타 레슨",
    image: "https://picsum.photos/600/400?random=6",
    price: 25000,
    category: "음악",
    dopamine: 82,
    currentParticipants: 3,
    maxParticipants: 5,
    location: "서울시 용산구",
    createdAt: "2024-03-10"
  },
  {
    id: 7,
    title: "보드게임 모임",
    description: "주말 보드게임 모임",
    image: "https://picsum.photos/600/400?random=7",
    price: 15000,
    category: "게임",
    dopamine: 95,
    currentParticipants: 6,
    maxParticipants: 8,
    location: "서울시 강서구",
    createdAt: "2024-03-09"
  },
  {
    id: 8,
    title: "독서 토론 모임",
    description: "월 1회 독서 토론",
    image: "https://picsum.photos/600/400?random=8",
    price: 10000,
    category: "독서",
    dopamine: 70,
    currentParticipants: 5,
    maxParticipants: 10,
    location: "서울시 송파구",
    createdAt: "2024-03-08"
  },
  {
    id: 13,
    title: "레고 조립 모임",
    description: "함께 만드는 레고 작품",
    image: "https://picsum.photos/600/400?random=13",
    price: 40000,
    category: "DIY",
    dopamine: 86,
    currentParticipants: 3,
    maxParticipants: 6,
    location: "서울시 성동구",
    createdAt: "2024-03-20"
  },
  {
    id: 14,
    title: "온라인 게임 파티",
    description: "롤 같이 하실 분!",
    image: "https://picsum.photos/600/400?random=14",
    price: 5000,
    category: "게임",
    dopamine: 94,
    currentParticipants: 3,
    maxParticipants: 5,
    location: "온라인",
    createdAt: "2024-03-21"
  },
  {
    id: 15,
    title: "여행 사진 촬영",
    description: "인생샷 찍어드립니다",
    image: "https://picsum.photos/600/400?random=15",
    price: 50000,
    category: "여행",
    dopamine: 89,
    currentParticipants: 2,
    maxParticipants: 4,
    location: "서울시 용산구",
    createdAt: "2024-03-22"
  },
  {
    id: 16,
    title: "한정판 피규어 교환회",
    description: "피규어 수집가들의 모임",
    image: "https://picsum.photos/600/400?random=16",
    price: 10000,
    category: "수집",
    dopamine: 87,
    currentParticipants: 8,
    maxParticipants: 15,
    location: "서울시 중구",
    createdAt: "2024-03-23"
  }
];
