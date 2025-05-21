#!/bin/bash

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

# 检查是否为 macOS
if [[ "$(uname)" != "Darwin" ]]; then
    echo -e "${RED}错误: 此脚本只能在 macOS 系统上运行${NC}"
    exit 1
fi

echo -e "${GREEN}开始设置预约自动化程序...${NC}"

# 检查 brew 是否安装
if ! command -v brew &> /dev/null; then
    echo -e "${YELLOW}正在安装 Homebrew...${NC}"
    /bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
fi

# 检查并安装 XQuartz
if ! command -v xquartz &> /dev/null; then
    echo -e "${YELLOW}正在安装 XQuartz...${NC}"
    brew install --cask xquartz
    echo -e "${RED}注意: 需要重启电脑以使 XQuartz 生效${NC}"
    echo -e "${RED}请重启后重新运行此脚本${NC}"
    exit 1
fi

# 检查并安装 Chrome（如果需要）
if [ ! -d "/Applications/Google Chrome.app" ]; then
    echo -e "${YELLOW}正在安装 Google Chrome...${NC}"
    brew install --cask google-chrome
fi

# 创建虚拟环境（如果不存在）
if [ ! -d ".venv" ]; then
    echo -e "${YELLOW}正在创建 Python 虚拟环境...${NC}"
    python3 -m venv .venv
fi

# 激活虚拟环境
echo -e "${YELLOW}正在激活虚拟环境...${NC}"
source .venv/bin/activate

# 安装依赖
echo -e "${YELLOW}正在安装依赖...${NC}"
pip install -r requirements.txt

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

# 检查 XQuartz 是否运行
if ! ps aux | grep -v grep | grep -q XQuartz; then
    echo -e "${YELLOW}正在启动 XQuartz...${NC}"
    open -a XQuartz
    sleep 3  # 等待 XQuartz 启动
fi

# 设置显示变量
export DISPLAY=:0

# 运行选项菜单
while true; do
    echo -e "\n${GREEN}请选择运行模式：${NC}"
    echo -e "1) ${YELLOW}前台运行（显示输出）${NC}"
    echo -e "2) ${YELLOW}后台运行（输出到日志）${NC}"
    echo -e "3) ${YELLOW}查看运行日志${NC}"
    echo -e "4) ${YELLOW}停止后台进程${NC}"
    echo -e "5) ${YELLOW}退出${NC}"
    read -p "请输入选项 (1-5): " choice

    case $choice in
        1)
            echo -e "${GREEN}开始前台运行程序...${NC}"
            python -m src
            ;;
        2)
            echo -e "${GREEN}开始后台运行程序...${NC}"
            nohup python -m src > logs/output.log 2>&1 &
            echo $! > .pid
            echo -e "${GREEN}程序已在后台启动，PID: $(cat .pid)${NC}"
            echo -e "使用选项 3 查看日志"
            ;;
        3)
            if [ -f "logs/output.log" ]; then
                tail -f logs/output.log
            else
                echo -e "${RED}日志文件不存在${NC}"
            fi
            ;;
        4)
            if [ -f ".pid" ]; then
                pid=$(cat .pid)
                kill $pid 2>/dev/null
                rm .pid
                echo -e "${GREEN}已停止进程 $pid${NC}"
            else
                echo -e "${RED}没有找到正在运行的后台进程${NC}"
            fi
            ;;
        5)
            echo -e "${GREEN}退出程序${NC}"
            exit 0
            ;;
        *)
            echo -e "${RED}无效的选项${NC}"
            ;;
    esac
done 