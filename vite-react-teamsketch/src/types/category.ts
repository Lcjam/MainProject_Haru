export interface CategoryState {
  categories: Array<{
    categoryId: number;
    categoryName: string;
  }>;
  hobbies: Array<{
    hobbyId: number;
    hobbyName: string;
  }>;
  loading: boolean;
  error: string | null;
  selectedCategoryId: number | null;
}
