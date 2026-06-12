interface FileResponse {
  status: string;
  data: {
    message: string;
    response: File | null;
  };
  code: string;
}

export type { FileResponse };
