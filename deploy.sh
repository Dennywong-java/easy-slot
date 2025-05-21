#!/bin/bash

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

# 检查是否为 Linux 系统
if [[ "$(uname)" != "Linux" ]]; then
    echo -e "${RED}错误: 此脚本只能在 Linux 系统上运行${NC}"
    exit 1
fi

echo -e "${GREEN}开始部署预约自动化程序...${NC}"

# 创建虚拟环境
echo -e "${YELLOW}正在创建 Python 虚拟环境...${NC}"
python3 -m venv .venv
source .venv/bin/activate

# 安装依赖
echo -e "${YELLOW}正在安装依赖...${NC}"
pip install -r requirements.txt

# 安装系统依赖
echo -e "${YELLOW}正在安装系统依赖...${NC}"
if command -v apt-get &> /dev/null; then
    sudo apt-get update
    sudo apt-get install -y xvfb firefox chromium-browser
elif command -v yum &> /dev/null; then
    sudo yum update
    sudo yum install -y xvfb firefox chromium
else
    echo -e "${RED}警告: 无法识别的包管理器，请手动安装 Xvfb、Firefox 和 Chromium${NC}"
fi

# 创建必要的目录
echo -e "${YELLOW}正在创建必要的目录...${NC}"
mkdir -p logs
mkdir -p browser/drivers

# 检查配置文件
if [ ! -f config.yml ]; then
    echo -e "${YELLOW}未找到配置文件，正在从示例创建...${NC}"
    cp config.example.yml config.yml
    echo -e "${RED}请编辑 config.yml 文件并填写必要的配置信息${NC}"
    exit 1
fi

# 创建系统服务
echo -e "${YELLOW}正在创建系统服务...${NC}"
sudo tee /etc/systemd/system/easy-slot.service << EOL
[Unit]
Description=Easy Slot Appointment Scheduler
After=network.target

[Service]
Type=simple
User=$(whoami)
WorkingDirectory=$(pwd)
Environment=DISPLAY=:99
ExecStartPre=/usr/bin/Xvfb :99 -screen 0 1024x768x24 -ac
ExecStart=$(pwd)/.venv/bin/python main.py
Restart=always
RestartSec=3

[Install]
WantedBy=multi-user.target
EOL

# 启用服务
echo -e "${YELLOW}正在启用服务...${NC}"
sudo systemctl daemon-reload
sudo systemctl enable easy-slot.service

echo -e "${GREEN}部署完成！${NC}"
echo -e "${YELLOW}使用以下命令管理服务：${NC}"
echo -e "启动服务：${GREEN}sudo systemctl start easy-slot${NC}"
echo -e "停止服务：${GREEN}sudo systemctl stop easy-slot${NC}"
echo -e "查看状态：${GREEN}sudo systemctl status easy-slot${NC}"
echo -e "查看日志：${GREEN}sudo journalctl -u easy-slot -f${NC}" 