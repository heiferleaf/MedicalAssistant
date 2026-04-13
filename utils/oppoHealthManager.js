/**
 * OPPO 健康数据管理工具类
 */
import SyncAPI from "../api/health.js";
class OppoHealthManager {
  constructor() {
    this.sdk = null;
    // 定义所有的类别
    this.allCategories = [
      "HEART_RATE_COUNT",
      "STEP_COUNT",
      "SLEEP_COUNT",
      "SLEEP_DETAIL",
      "BLOOD_OXYGEN_COUNT",
      "RELAX_COUNT",
      "RELAX_DETAIL",
      "PRESSURE_COUNT",
      "PRESSURE_DETAIL",
      "DAILY_ACTIVITY_DETAIL",
      "BLOOD_PRESSURE_COUNT",
      "BLOOD_PRESSURE_DETAIL",
    ];
  }

  // 获取插件单例
  getSdk() {
    if (!this.sdk) {
      this.sdk = uni.requireNativePlugin("OppoHealthModule");
    }
    return this.sdk;
  }

  // 1. 初始化 SDK
  async init() {
    const sdk = this.getSdk();
    return new Promise((resolve, reject) => {
      if (!sdk) return reject("插件未加载");
      sdk.initSdk((res) => {
        console.log("[OppoHealth] Init:", res);
        resolve(res);
      });
    });
  }

  // 2. 授权逻辑 (包含自动引导安装)
  async auth() {
    const sdk = this.getSdk();
    return new Promise((resolve, reject) => {
      sdk.requestAuth((code) => {
        if (code === 100000) {
          resolve(true);
        } else if (code === 100007) {
          uni.showModal({
            title: "需要安装健康App",
            content: "为了同步您的健康数据，需要配合“欢太健康”App使用。",
            confirmText: "去安装",
            success: (res) => {
              if (res.confirm) sdk.goToInstallHealthApp();
            },
          });
          reject("HEALTH_APP_NOT_INSTALLED");
        } else {
          reject(code);
        }
      });
    });
  }

  // 3. 读取指定类型的健康数据
  async readData(type, days = 1) {
    const sdk = this.getSdk();
    return new Promise((resolve, reject) => {
      sdk.readHealthData(type, days, (res) => {
        if (typeof res === "number") {
          reject({ type, code: res });
        } else {
          try {
            resolve(JSON.parse(res));
          } catch (e) {
            resolve(res); // 解析失败返回原始字符串
          }
        }
      });
    });
  }

  /**
   * 4. 核心功能：聚合所有数据并上传后端
   * 这个函数可以直接被定时器或 OnShow 调用
   */
  async syncToBackend(userId) {
    if (!userId) return;

    try {
      await this.init();
      // 获取今天的数据（1天内）
      const [heart, steps, oxygen, sleep] = await Promise.all([
        this.readData("heart_rate", 1).catch(() => []),
        this.readData("step_count", 1).catch(() => []),
        this.readData("blood_oxygen", 1).catch(() => []),
        this.readData("sleep", 1).catch(() => []),
      ]);

      // 提取最新值或平均值（根据后端字段需求）
      const payload = {
        userId: userId,
        heartRate: heart.length > 0 ? heart[0].value : null,
        stepCount: steps.length > 0 ? steps[0].value : null,
        bloodOxygen: oxygen.length > 0 ? oxygen[0].value : null,
        // 睡眠通常取第一条（最晚结束的那条）
        sleepDuration: sleep.length > 0 ? sleep[0].duration / 3600 : null,
        measureTime: new Date().toISOString(),
      };

      // 只有当有数据时才上传
      if (Object.values(payload).some((v) => v !== null && v !== userId)) {
        return await this.upload(payload);
      }
    } catch (e) {
      console.error("[OppoHealth] Sync Error:", e);
    }
  }

  /**
   * 核心新增函数：全量读取并缓存数据
   * @param {Number} queryDays 查询天数，默认15天
   */
  async fetchAllAndCache(queryDays = 15) {
    try {
      await this.init();

      // 1. 并发读取所有类型的数据
      const results = await Promise.all(
        this.allCategories.map((cat) => this.readData(cat, queryDays)),
      );

      // 2. 映射整合数据
      const fullData = {};
      this.allCategories.forEach((category, index) => {
        // 根据你的要求，Key 保持与 category 对应
        // 特殊处理：HEART_RATE_COUNT 映射为 Heart_Rate_Count (按你提供的示例)
        const storageKey = category;
        fullData[storageKey] = results[index];
      });

      // 3. Stringify 并存入 Storage
      const jsonStr = JSON.stringify(fullData);
      console.log("[OppoHealth] 全量数据字符串:", jsonStr);
      this.consoleLog("全量数据字符串: " + jsonStr);
      uni.setStorageSync("OPPO_HEALTH_FULL_DATA", jsonStr);

      console.log("[OppoHealth] 全量数据已更新至 Storage");
      return fullData;
    } catch (e) {
      console.error("[OppoHealth] 全量读取失败:", e);
      throw e;
    }
  }

  // 内部上传接口
  async upload(data) {
    return SyncAPI.sync(data);
  }

  testAPI() {
    return "这是一个测试函数，可以在页面中调用来验证插件是否正常工作";
  }

  async consoleLog(info) {
    await this.init();
    const sdk = this.getSdk();
    return new Promise((resolve, reject) => {
      sdk.consoleLog(info, (res) => {
        resolve(res);
      });
    });
  }
}

export default new OppoHealthManager();
