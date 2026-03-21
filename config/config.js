// config.js

// config.js 头部添加常量
const STORAGE_KEY = 'SYSTEM_NOTIFICATIONS';
const MAX_MSG_COUNT = 100; // 最大保存100条

// 获取本地存储的消息
function getLocalMessages() {
	return uni.getStorageSync(STORAGE_KEY) || [];
}// 核心逻辑：保存消息并更新未读状态
function saveAndNotify(newMsg) {
	let list = getLocalMessages();
	
	// 1. 插入新消息到开头
	const msgItem = {
		...newMsg,
		id: Date.now(),
		time: new Date().toLocaleString(),
		isRead: false // 标记为未读
	};
	list.unshift(msgItem);
	
	// 2. 限制数量
	if (list.length > MAX_MSG_COUNT) {
		list = list.slice(0, MAX_MSG_COUNT);
	}
	
	// 3. 写入持久化存储
	uni.setStorageSync(STORAGE_KEY, list);
	
	// 4. 通知全局：有新消息（用于首页红点）
	uni.$emit('NEW_NOTIFICATION_RECEIVED', { hasUnread: true });
}



let BASE_URL = "";
let WS_BASE_URL = "";

const USE_SIMULATOR = true; // 是否使用模拟器

if (process.env.NODE_ENV === "production") {
  // 开发环境：点击“运行”时生效
  // 可以是 localhost，也可以是局域网 IP
  BASE_URL = "http://8.148.94.242:8080/api";
  WS_BASE_URL = "ws://8.148.94.242:8080/ws";
} else if (process.env.NODE_ENV === "development" && USE_SIMULATOR) {
  // 生产环境：点击“发行”打包时自动生效
  BASE_URL = "http://8.148.94.242:8080/api";
  WS_BASE_URL = "ws://8.148.94.242:8080/ws";
} else {
  BASE_URL = "http://localhost:8080/api";
  WS_BASE_URL = "ws://localhost:8080/ws";
}

export { BASE_URL, WS_BASE_URL };
let socketTask = null;
let heartbeatInterval = null;

export function connect() {
  const token = uni.getStorageSync("accessToken");
  if (!token) return;

  // 1. 发起握手 [cite: 302]
  socketTask = uni.connectSocket({
    url: `${WS_BASE_URL}?token=${token}`,
    complete: () => {},
  });

  // 2. 监听连接打开 [cite: 332]
  uni.onSocketOpen(() => {
    console.log("WS 连接已打开");
    startHeartbeat(); // 开启心跳 [cite: 333]
  });

  // 3. 监听消息接收 [cite: 325]
  uni.onSocketMessage((res) => {
    console.log("WS 收到消息:", res.data);
    if (res.data != "pong") {
      const pushData = JSON.parse(res.data);
      handleMessage(pushData);
    }
  });

  // 4. 监听错误（处理 Token 过期） [cite: 323]
  uni.onSocketError((err) => {
    console.log("WS 连接失败，尝试刷新 Token...");
    // 这里应调用你的刷新 token 逻辑 [cite: 324]
    // 刷新成功后 setTimeout(() => connect(), 2000); [cite: 324]
  });

  // 5. 监听关闭 [cite: 315]
  uni.onSocketClose(() => {
    clearInterval(heartbeatInterval);
  });
}

// 心跳保持连接 [cite: 327]
function startHeartbeat() {
  clearInterval(heartbeatInterval);
  heartbeatInterval = setInterval(() => {
    uni.sendSocketMessage({
      data: "ping",
      fail: () => connect(), // 发送失败则尝试重连 [cite: 330]
    });
  }, 30000); // 30秒一次 [cite: 331]
}

function handleMessage(pushData) {
  const { type } = pushData;
  console.log("收到推送消息:", type, pushData);

  let title = "系统提醒";
  let content = "";
  // 1. 根据不同类型匹配并动态拼接通知文案
  switch (type) {
    case "medicine_alarm":
      title = "健康告警 ⚠️";
      // 提取：姓名、药品名
      const alarmMember = pushData.memberName || "有成员";
      const alarmMedicine = pushData.medicineName || "药物";
      content = `${alarmMember} 漏服了 [${alarmMedicine}]，请及时提醒他服药哦！`;
      break;

    case "medicine_update":
      title = "健康数据更新";
      // 提取：姓名、药品名、状态
      const updateMember = pushData.memberName || "有成员";
      const updateMedicine = pushData.medicineName || "药物";
      const statusText = pushData.status === 1 ? "已完成服用" : "更新了服药状态";
      content = `${updateMember} 的 [${updateMedicine}] ${statusText}`;
      break;

    case "join_success":
      title = "家庭新成员";
      // 提取：新成员昵称
      const newMember = pushData.targetNickname || "新用户";
      content = `欢迎新成员 [${newMember}] 加入家庭组！`;
      break;

    case "member_leave":
      title = "成员变动";
      // 提取：退出成员昵称
      const leaveMember = pushData.targetNickname || "成员";
      content = `成员 [${leaveMember}] 已退出当前家庭组`;
      break;

    default:
      title = "新消息";
      content = "您有一条新的健康云通知";
  }
  // 持久化存储
  saveAndNotify({ title, content, type, raw: pushData });

  // 2. 触发通知栏消息（App 端）
  // #ifdef APP-PLUS
  if (content) {
    plus.push.createMessage(content, JSON.stringify(pushData), {
      title: title,
      cover: false, // 建议设为 false，这样多条消息不会互相覆盖，用户可以滑动手按序查看
    });
  }
  // #endif

  // 3. 处理业务逻辑 (保持原有 emit 逻辑)
  // 你可以将逻辑合并到上面的 switch 中，或者保持独立
  const businessLogic = {
    medicine_update: () => uni.$emit("REFRESH_HEALTH_DATA", pushData),
    join_success: () => uni.$emit("REFRESH_MEMBER_LIST", pushData),
    member_leave: () => uni.$emit("REFRESH_MEMBER_LIST", pushData),
  };

  if (businessLogic[type]) businessLogic[type]();

  // 核心：通知前端页面实时刷新
  // 我们根据 type 定义不同的事件名，或者统一用一个事件名
  switch (type) {
    case "medicine_alarm":
    case "medicine_update":
      // 提醒健康数据页面刷新，并把最新的数据 pushData 传过去
      uni.$emit("REFRESH_HEALTH_DATA", pushData);
      break;

    case "join_success":
    case "member_leave":
      // 提醒家庭成员列表页面刷新
      uni.$emit("REFRESH_MEMBER_LIST", pushData);
      break;

    default:
      console.log("未定义处理逻辑的消息类型:", type);
  }
}

// 主动关闭连接 [cite: 335]
export function closeConnection() {
  clearInterval(heartbeatInterval);
  uni.closeSocket();
}

export default { connect, closeConnection };
