from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    app_env: str = "development"
    app_name: str = "mtc-ai-service"
    host: str = "0.0.0.0"
    port: int = 8000

    # Redis
    redis_host: str = "localhost"
    redis_port: int = 6379

    # 数据库（骨架阶段不用，预留）
    database_url: str = ""

    class Config:
        env_file = ".env"


settings = Settings()
