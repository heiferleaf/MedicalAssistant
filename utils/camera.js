// 引入原生插件（注意：在非 App 环境下此调用会报错，需做好容错）
const cameraModule = uni.requireNativePlugin("yun-camerax-module");

/**
 * 通用拍照并跳转识别逻辑
 * @param {Object} options 自定义配置
 * @param {string} options.text 扫描框提示文字
 * @param {boolean} options.autoJump 是否自动跳转到 AI 助手页，默认为 true
 */
export const takeDrugPhoto = (options = {}) => {
  return new Promise((resolve, reject) => {
    // 1. 调用原生插件拍照
    cameraModule.takePhoto(
      {
        type: options.type || 0,
        imageIndex: options.imageIndex || 1,
        text: options.text || "请正对药盒拍摄\n确保药品信息清晰可见",
        landscape: options.landscape || false,
      },
      (res) => {
        console.log("原生插件返回原始数据:", res);

        if (res && res.file) {
          const finalPath = "file://" + res.file;

          // 2. 统一存储逻辑
          uni.setStorageSync("last_scan_image", finalPath);

          // 3. 统一跳转逻辑
          if (options.autoJump !== false) {
            uni.navigateTo({
              url: "/pages/ai/Assistant?from=scan",
              success: () => {
                uni.hideLoading();
              },
            });
          }

          // 返回结果给调用者
          resolve(finalPath);
        } else {
          // 用户取消或拍摄失败
          console.warn("拍照未完成或取消");
          reject(new Error("User cancelled or failed"));
        }
      }
    );
  });
};

export default {
  takeDrugPhoto
};