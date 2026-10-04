from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    app_env: str = "development"
    app_name: str = "mtc-ai-service"
    host: str = "0.0.0.0"
    port: int = 8000

    # Redis
    redis_host: str = "localhost"
    redis_port: int = 6379

    # 数据库
    database_url: str = "postgresql://mtc:mtc_password_change_me@localhost:5432/mtc"

    # DeepSeek LLM
    deepseek_api_key: str = ""
    deepseek_base_url: str = "https://api.deepseek.com/v1"
    deepseek_model: str = "deepseek-chat"
    deepseek_embed_model: str = "deepseek-embedding"
    llm_temperature: float = 0.7
    llm_max_tokens: int = 4096

    # RAG
    rag_chunk_size: int = 500
    rag_chunk_overlap: int = 50
    rag_top_k: int = 5
    rag_embedding_dim: int = 1536

    # 文件上传
    upload_dir: str = "./uploads"
    max_file_size_mb: int = 20

    class Config:
        env_file = ".env"


settings = Settings()
