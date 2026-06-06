from pydantic import BaseModel


class FileResponse(BaseModel):
    id: int
    file_name: str
    file_type: str | None
    file_size: int
    created_at: str


class FileUploadResponse(FileResponse):
    message: str
