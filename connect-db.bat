@echo off
chcp 65001 > nul
echo ========================================================
echo  [SoftCampus] EC2 DB 터널링 연결 스크립트
echo ========================================================
echo.
echo  [설명]
echo  로컬 포트 3307을 통해 원격 EC2 DB(3306)에 안전하게 접속합니다.
echo  이 창을 닫으면 연결이 끊어집니다.
echo.
echo  [사용법]
echo  1. swcampus-key.pem 파일이 이 스크립트와 같은 폴더에 있어야 합니다.
echo  2. 접속 후 로컬 DB 툴에서 Host: localhost, Port: 3307 로 접속하세요.
echo.
echo ========================================================

:: 설정 변수 (팀원들과 공유 시 IP만 변경하면 됨)
set EC2_IP=3.34.123.123
set KEY_FILE=swcampus-key.pem
set LOCAL_PORT=3307
set REMOTE_PORT=3306

:: 키 파일 존재 확인
if not exist "%KEY_FILE%" (
    echo [오류] %KEY_FILE% 파일을 찾을 수 없습니다.
    echo 스크립트와 같은 폴더에 키 파일을 넣어주세요.
    pause
    exit /b
)

echo.
echo [연결 시도] %EC2_IP% 서버로 터널링을 시작합니다...
echo (처음 접속 시 'yes'를 입력해야 할 수 있습니다)
echo.

:: SSH 터널링 실행
ssh -i "%KEY_FILE%" -N -L %LOCAL_PORT%:127.0.0.1:%REMOTE_PORT% ubuntu@%EC2_IP%

:: 연결 종료 시
echo.
echo [연결 종료] 터널링이 종료되었습니다.
pause
