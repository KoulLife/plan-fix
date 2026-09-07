const apiBaseUrl = import.meta.env.VITE_API_BASE_URL?.replace(/\/$/, "");

export interface UploadImageResult {
  imageUrl: string;
}

/**
 * 이미지 파일을 백엔드/S3로 업로드하고 접근 가능한 영구 URL을 받아옵니다.
 */
export async function uploadImageFile(file: File): Promise<UploadImageResult> {
  if (!apiBaseUrl) {
    throw new Error("API URL이 설정되지 않았습니다.");
  }

  const formData = new FormData();
  formData.append("file", file);

  const response = await fetch(`${apiBaseUrl}/images/upload`, {
    method: "POST",
    credentials: "include",
    body: formData,
  });

  if (!response.ok) {
    const errorBody = await response.text().catch(() => "");
    try {
      const parsed = JSON.parse(errorBody);
      if (parsed.message) {
        throw new Error(parsed.message);
      }
    } catch {
      // JSON 파싱 실패 시 일반 에러 사용
    }
    throw new Error(errorBody || "이미지 업로드에 실패했습니다.");
  }

  return (await response.json()) as UploadImageResult;
}
