$server = "47.96.152.231"
$password = "Ssy123qwert"
$jarLocal = "e:\Codebuddy-work space\campus-trade\campus-trade-server\target\campus-trade-server-1.0.0.jar"
$sqlLocal = "e:\Codebuddy-work space\campus-trade\init.sql"
$remoteDir = "/root/campus-trade"

Write-Host "=== [1/5] Install sshpass ==="
# Download sshpass for Windows
$sshpassUrl = "https://github.com/xhcgithub/sshpass-win32/releases/download/v1.0.0/sshpass.exe"
$sshpassPath = "C:\ssh-keys\sshpass.exe"
if (!(Test-Path $sshpassPath)) {
    Invoke-WebRequest -Uri $sshpassUrl -OutFile $sshpassPath -UseBasicParsing
}
Write-Host "OK"

Write-Host "=== [2/5] Upload jar to server ==="
& $sshpassPath -p $password scp -o StrictHostKeyChecking=no $jarLocal root@${server}:${remoteDir}/campus-trade-server.jar 2>&1
Write-Host "OK"

Write-Host "=== [3/5] Upload init.sql ==="
& $sshpassPath -p $password scp -o StrictHostKeyChecking=no $sqlLocal root@${server}:${remoteDir}/init.sql 2>&1
Write-Host "OK"

Write-Host "=== [4/5] Setup server environment ==="
$setupScript = @'
set -e
echo "--- Update system ---"
yum install -y wget curl unzip > /dev/null 2>&1

echo "--- Install Java 22 ---"
if ! java -version 2>&1 | grep -q "22"; then
    cd /tmp
    wget -q https://download.oracle.com/java/22/latest/jdk-22_linux-x64_bin.tar.gz
    tar -xzf jdk-22_linux-x64_bin.tar.gz -C /usr/local/
    ln -sf /usr/local/jdk-22*/bin/java /usr/bin/java
    ln -sf /usr/local/jdk-22*/bin/javac /usr/bin/javac
    echo 'export JAVA_HOME=/usr/local/jdk-22' > /etc/profile.d/java.sh
    echo 'export PATH=$JAVA_HOME/bin:$PATH' >> /etc/profile.d/java.sh
    source /etc/profile.d/java.sh
fi
java -version 2>&1
echo "Java OK"

echo "--- Install MySQL 8 ---"
if ! command -v mysql &> /dev/null; then
    yum install -y mysql80-community-release-el7-3.noarch.rpm > /dev/null 2>&1 || true
    yum install -y mysql-community-server > /dev/null 2>&1
fi
systemctl start mysqld
systemctl enable mysqld
echo "MySQL started"

echo "--- Setup MySQL root password ---"
temp_pass=$(grep 'temporary password' /var/log/mysqld.log 2>/dev/null | tail -1 | awk '{print $NF}')
if [ -n "$temp_pass" ]; then
    mysql --connect-expired-password -uroot -p"$temp_pass" -e "ALTER USER 'root'@'localhost' IDENTIFIED BY '123456';" 2>/dev/null || true
    mysql --connect-expired-password -uroot -p"$temp_pass" -e "set global validate_password.policy=0; set global validate_password.length=4;" 2>/dev/null || true
    mysql --connect-expired-password -uroot -p"$temp_pass" -e "ALTER USER 'root'@'localhost' IDENTIFIED BY '123456';" 2>/dev/null || true
else
    # Try without password
    mysql -uroot -e "ALTER USER 'root'@'localhost' IDENTIFIED BY '123456';" 2>/dev/null || true
fi
# Final verify
mysql -uroot -p'123456' -e "SELECT 1" 2>&1 && echo "MySQL root password set to 123456" || echo "MySQL password already set"

echo "--- Create database ---"
mysql -uroot -p'123456' -e "CREATE DATABASE IF NOT EXISTS campus_trade DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;" 2>&1
mysql -uroot -p'123456' campus_trade < /root/campus-trade/init.sql 2>&1
echo "Database initialized"

echo "--- Create uploads directory ---"
mkdir -p /root/campus-trade/uploads

echo "--- Setup firewall ---"
systemctl stop firewalld 2>/dev/null || true
systemctl disable firewalld 2>/dev/null || true
echo "Firewall disabled (use security group instead)"

echo "=== SETUP COMPLETE ==="
'@

$setupScript | & $sshpassPath -p $password ssh -o StrictHostKeyChecking=no root@${server} "cat > /root/setup.sh && bash /root/setup.sh" 2>&1
Write-Host "Setup done"

Write-Host "=== [5/5] Start application ==="
$startScript = @'
cd /root/campus-trade
# Kill old process
pkill -f campus-trade-server.jar 2>/dev/null || true
sleep 2
# Start new
nohup java -jar /root/campus-trade/campus-trade-server.jar --spring.datasource.url="jdbc:mysql://localhost:3306/campus_trade?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai" --campustrade.upload.path="/root/campus-trade/uploads" > /root/campus-trade/app.log 2>&1 &
sleep 5
echo "Process:"
ps aux | grep campus-trade-server | grep -v grep
echo "---"
echo "Recent logs:"
tail -20 /root/campus-trade/app.log
'@

$startScript | & $sshpassPath -p $password ssh -o StrictHostKeyChecking=no root@${server} "bash" 2>&1
Write-Host "=== DEPLOY COMPLETE ==="
Write-Host "Server: http://47.96.152.231:8080"
