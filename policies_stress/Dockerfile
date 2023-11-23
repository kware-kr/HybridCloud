# ref: https://jiyeonseo.github.io/2022/10/24/uvicorn-gunicorn-fastapi-docker/
FROM tiangolo/uvicorn-gunicorn-fastapi:python3.11-slim

# ref: https://velog.io/@jaybon/도커로-서버-배포-fastapi
RUN ln -snf /usr/share/zoneinfo/Asia/Seoul /etc/localtime
RUN echo Asia/Seoul > /etc/timezone

# ref: https://fastapi.tiangolo.com/deployment/docker/
WORKDIR /app

#
RUN	apt-get update && apt-get install -y \
    libpq-dev \
    python3-dev \
    gcc

#
COPY ./requirements.txt /app/requirements.txt

#
RUN pip install --no-cache-dir --upgrade -r /app/requirements.txt

#
COPY ./app /app

#
CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "80"]