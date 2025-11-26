#!/bin/bash

# ==============================================================================
# [Let's Encrypt 초기 설정 스크립트]
#
# 목적: SSL 인증서 발급 자동화
# 사용법: ./init-letsencrypt.sh
# ==============================================================================

# 스크립트가 있는 디렉토리로 이동 (어디서 실행하든 docker-compose.yml을 찾을 수 있게)
cd "$(dirname "$0")"

if ! docker compose version > /dev/null 2>&1; then
  echo 'Error: docker compose is not installed.' >&2
  exit 1
fi

# Root 권한 체크
if [ "$EUID" -ne 0 ]; then 
  echo "Error: Please run as root (sudo ./init-letsencrypt.sh)"
  exit 1
fi

# .env 파일 로드 (존재할 경우)
if [ -f .env ]; then
  export $(cat .env | grep -v '#' | xargs)
elif [ -f ../../.env ]; then
  export $(cat ../../.env | grep -v '#' | xargs)
fi

domains="${DOMAIN_NAME:-softwarecampus.earlydreamer.dev}"
rsa_key_size=4096
data_path="./data/certbot"
email="${CERT_EMAIL:-earlydreamer@naver.com}" # 이메일 주소 입력
staging=0 # 테스트 시 1, 실제 발급 시 0

if [ -d "$data_path" ]; then
  echo "Existing data found for $domains. Continue and replace existing certificate? (y/N) "
  read decision
  if [ "$decision" != "Y" ] && [ "$decision" != "y" ]; then
    exit
  fi
fi


if [ ! -e "$data_path/conf/options-ssl-nginx.conf" ] || [ ! -e "$data_path/conf/ssl-dhparams.pem" ]; then
  echo "### Downloading recommended TLS parameters ..."
  mkdir -p "$data_path/conf"
  curl -s https://raw.githubusercontent.com/certbot/certbot/master/certbot-nginx/certbot_nginx/_internal/tls_configs/options-ssl-nginx.conf > "$data_path/conf/options-ssl-nginx.conf"
  curl -s https://raw.githubusercontent.com/certbot/certbot/master/certbot/certbot/ssl-dhparams.pem > "$data_path/conf/ssl-dhparams.pem"
  echo
fi

echo "### Creating dummy certificate for $domains ..."
path="/etc/letsencrypt/live/$domains"
mkdir -p "$data_path/conf/live/$domains"
docker compose run --rm --entrypoint "\
  openssl req -x509 -nodes -newkey rsa:$rsa_key_size -days 1\
    -keyout '$path/privkey.pem' \
    -out '$path/fullchain.pem' \
    -subj '/CN=localhost'" softcampus-certbot
echo


echo "### Starting nginx ..."
docker compose up --force-recreate -d softcampus-nginx
echo "Waiting for Nginx to initialize..."
sleep 10
echo

echo "### Deleting dummy certificate for $domains ..."
docker compose run --rm --entrypoint "\
  rm -Rf /etc/letsencrypt/live/$domains && \
  rm -Rf /etc/letsencrypt/archive/$domains && \
  rm -Rf /etc/letsencrypt/renewal/$domains.conf" softcampus-certbot
echo


echo "### Requesting Let's Encrypt certificate for $domains ..."
domain_args=""
for domain in $domains; do
  domain_args="$domain_args -d $domain"
done

# Select appropriate email arg
case "$email" in
  "") email_arg="--register-unsafely-without-email" ;;
  *) email_arg="-m $email" ;;
esac

# Enable staging mode if needed
if [ "$staging" != "0" ]; then staging_arg="--staging"; fi

docker compose run --rm --entrypoint "\
  certbot certonly --webroot -w /var/www/certbot \
    $staging_arg \
    $email_arg \
    $domain_args \
    --rsa-key-size $rsa_key_size \
    --agree-tos \
    --force-renewal" softcampus-certbot
echo

echo "### Reloading nginx ..."
# Nginx가 완전히 실행될 때까지 대기
echo "Waiting for Nginx to start..."
until docker compose exec softcampus-nginx nginx -t; do
  echo "Nginx is not ready yet..."
  sleep 2
done
docker compose exec softcampus-nginx nginx -s reload
