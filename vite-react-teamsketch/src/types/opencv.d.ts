declare module 'opencv.js' {
  interface Mat {
    rows: number;
    cols: number;
    delete(): void;
  }

  interface MatVector {
    size(): number;
    get(index: number): Mat;
    delete(): void;
  }

  interface Size {
    width: number;
    height: number;
  }

  interface Point {
    x: number;
    y: number;
  }

  interface Rect {
    x: number;
    y: number;
    width: number;
    height: number;
  }

  interface OpenCV {
    Mat: new () => Mat;
    MatVector: new () => MatVector;
    Size: new (width: number, height: number) => Size;
    Point: new (x: number, y: number) => Point;
    Rect: new (x: number, y: number, width: number, height: number) => Rect;
    COLOR_RGBA2GRAY: number;
    COLOR_BGR2GRAY: number;
    COLOR_RGB2GRAY: number;
    BORDER_DEFAULT: number;
    RETR_EXTERNAL: number;
    CHAIN_APPROX_SIMPLE: number;
    onRuntimeInitialized: () => void;
    imread(canvas: HTMLCanvasElement): Mat;
    imshow(canvas: HTMLCanvasElement, mat: Mat): void;
    cvtColor(src: Mat, dst: Mat, code: number, dstCn?: number): void;
    GaussianBlur(src: Mat, dst: Mat, ksize: Size, sigmaX: number, sigmaY: number, borderType: number): void;
    Canny(src: Mat, dst: Mat, threshold1: number, threshold2: number): void;
    findContours(src: Mat, contours: MatVector, hierarchy: Mat, mode: number, method: number): void;
    approxPolyDP(curve: Mat, dst: Mat, epsilon: number, closed: boolean): void;
    arcLength(curve: Mat, closed: boolean): number;
    boundingRect(contour: Mat): Rect;
  }

  const cv: OpenCV;
  export = cv;
} 