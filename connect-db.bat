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
set EC2_IP=서버 IP입력
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

:: 권한 자동 수정 (Windows 전용)
echo [권한 수정] %KEY_FILE% 권한을 현재 사용자로 제한합니다...
icacls "%KEY_FILE%" /reset > nul
icacls "%KEY_FILE%" /grant:r "%USERNAME%":"R" > nul
icacls "%KEY_FILE%" /inheritance:r > nul


echo.
echo [연결 시도] %EC2_IP% 서버로 터널링을 시작합니다...
echo ----------------------------------------------------------------
echo  * 별도의 창에서 SSH 터널링이 실행됩니다.
echo  * 해당 창을 닫으면 연결이 종료됩니다.
echo ----------------------------------------------------------------
echo.

:: SSH 터널링을 별도 창에서 실행 (창 제목 설정)
start "SoftCampus DB Tunneling" ssh -i "%KEY_FILE%" -N -L %LOCAL_PORT%:127.0.0.1:%REMOTE_PORT% ubuntu@%EC2_IP%

echo [상태 확인] 연결을 확인하는 중입니다 (3초 대기)...
timeout /t 3 /nobreak > nul

:: 포트 리스닝 확인
netstat -an | find "%LOCAL_PORT%" | find "LISTENING" > nul
if %errorlevel% equ 0 (
    echo.
    echo [성공] 터널링이 성공적으로 연결되었습니다!
    echo [접속 정보] Host: localhost, Port: %LOCAL_PORT%
    echo.
    echo  * 터널링 창을 닫지 마세요.
) else (
    echo.
    echo [실패] 터널링 연결에 실패했습니다.
    echo  * 키 파일 권한이나 네트워크 상태를 확인해주세요.
    echo  * 새로 열린 창의 에러 메시지를 확인하세요.
)

pause
