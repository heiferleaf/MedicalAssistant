/**
 * 图片处理方法模块
 * 封装所有图片处理相关的方法（OCR、Base64 转换等）
 */

export default {
  methods: {
    /**
     * 将图片路径转换为 Base64（App 端专用）
     * @param {string} imagePath - 图片路径
     * @returns {Promise<string>} Base64 数据
     */
    convertImagePathToBase64(imagePath) {
      return new Promise((resolve, reject) => {
        try {
          plus.io.resolveLocalFileSystemURL(imagePath, (entry) => {
            entry.file((file) => {
              const reader = new plus.io.FileReader();
              reader.onload = (e) => {
                resolve(e.target.result);
              };
              reader.onerror = (err) => {
                reject(err);
              };
              reader.readAsDataURL(file);
            }, (err) => {
              reject(err);
            });
          }, (err) => {
            reject(err);
          });
        } catch (e) {
          reject(e);
        }
      });
    },
    
    /**
     * OCR 识别成功处理
     * @param {object} data - OCR 识别结果
     */
    handleOCRSuccess(data) {
      console.log('OCR 识别成功:', data);
      
      // 检查后端返回的状态
      if (data.status === 'error') {
        // 后端返回错误（例如：too many values to unpack）
        console.error('后端 OCR 处理异常:', data.message);
        
        // 直接调用失败处理
        this.handleOCRError(new Error(data.message || 'OCR 处理异常'));
        return;
      }
      
      // 正常情况：提取 OCR 结果
      this.ocrResult = data.output || data.ocr_result || '';
      this.ocrLoading = false;
      
      console.log('✅ OCR 识别结果长度:', this.ocrResult.length);
    },
    
    /**
     * OCR 识别失败处理
     * @param {object} error - 错误信息
     */
    handleOCRError(error) {
      console.error('OCR 识别失败:', error);
      this.ocrResult = '';
      this.ocrLoading = false;
      uni.showToast({
        title: 'OCR 识别失败，请重试',
        icon: 'none'
      });
    },
    
    /**
     * 将图片转换为 Base64
     * @param {string} imagePath - 图片路径
     * @returns {Promise<string>} Base64 数据
     */
    async imageToBase64(imagePath) {
      // #ifdef APP-PLUS
      return await this.convertImagePathToBase64(imagePath);
      // #endif
      
      // #ifdef H5
      return await new Promise((resolve, reject) => {
        const reader = new FileReader();
        reader.onload = (e) => resolve(e.target.result);
        reader.onerror = (e) => reject(e);
        reader.readAsDataURL(imagePath);
      });
      // #endif
    },
    
    /**
     * 将 Base64 转换为临时文件
     * @param {string} base64Data - Base64 数据
     * @returns {Promise<string>} 临时文件路径
     */
    async base64ToFile(base64Data) {
      // #ifdef APP-PLUS
      // App 端：使用 plus.io 写入临时文件
      const fileName = `temp_${Date.now()}.jpg`;
      const filePath = `_doc/ocr_temp/${fileName}`;
      
      try {
        // 创建目录
        plus.io.resolveLocalFileSystemURL('_doc/ocr_temp', 
          (entry) => {
            // 目录已存在
          }, 
          (err) => {
            // 目录不存在，创建
            plus.io.requestFileSystem(plus.io.PRIVATE_DOC, (fs) => {
              fs.root.getDirectory('ocr_temp', { create: true });
            });
          }
        );
        
        // 写入文件
        const base64WithoutPrefix = base64Data.split(',')[1] || base64Data;
        const arrayBuffer = new ArrayBuffer(base64WithoutPrefix.length * 2);
        const view = new Uint8Array(arrayBuffer);
        for (let i = 0; i < base64WithoutPrefix.length; i++) {
          view[i] = base64WithoutPrefix.charCodeAt(i);
        }
        
        plus.io.resolveLocalFileSystemURL(filePath, 
          (entry) => {
            // 文件已存在，删除
            entry.remove();
          },
          () => {
            // 文件不存在，继续
          }
        );
        
        plus.io.resolveLocalFileSystemURL('_doc/ocr_temp', (entry) => {
          entry.getFile(fileName, { create: true }, (fileEntry) => {
            fileEntry.createWriter((writer) => {
              writer.write(arrayBuffer);
            });
          });
        });
        
        return filePath;
      } catch (e) {
        console.error('Base64 转文件失败:', e);
        throw e;
      }
      // #endif
      
      // #ifdef H5
      // H5 端：返回 Blob
      const blob = this.base64ToBlob(base64Data);
      return new File([blob], 'temp.jpg', { type: 'image/jpeg' });
      // #endif
    },
    
    /**
     * 将 Base64 转换为 Blob
     * @param {string} base64Data - Base64 数据
     * @param {string} type - MIME 类型
     * @returns {Blob} Blob 对象
     */
    base64ToBlob(base64Data, type = 'image/jpeg') {
      // 去掉前缀
      let cleanBase64 = base64Data;
      if (cleanBase64.startsWith('data:')) {
        cleanBase64 = cleanBase64.split(',')[1];
      }
      
      const byteCharacters = atob(cleanBase64);
      const byteNumbers = new Array(byteCharacters.length);
      for (let i = 0; i < byteCharacters.length; i++) {
        byteNumbers[i] = byteCharacters.charCodeAt(i);
      }
      const byteArray = new Uint8Array(byteNumbers);
      return new Blob([byteArray], { type: type });
    },
    
    /**
     * 处理相机按钮（拍照识别）
     */
    handleCamera() {
      console.log('点击相机按钮，跳转拍照识别');
      uni.navigateTo({
        url: '/pages/scan/DrugScan?from=chat',
        animationType: 'fade-in',
        animationDuration: 300
      });
    },
    
    /**
     * 移除图片
     */
    removeImage() {
      // #ifdef H5
      // H5 端：清理缓存的图片数据
      if (this.scanImage && this.scanImage.includes('/images/drug_')) {
        const fileName = this.scanImage.split('/').pop();
        uni.removeStorageSync('drug_image_' + fileName);
        console.log('清理图片缓存:', fileName);
      }
      // #endif
      
      this.scanImage = '';
      this.scanImageBase64 = '';
      this.showImagePreview = false;
      uni.removeStorageSync('last_scan_image');
    },
    
    /**
     * Base64 转 ArrayBuffer（用于 App 端文件写入）
     */
    base64ToArrayBuffer(base64Data) {
      // 去掉前缀（如果有）
      if (base64Data.startsWith('data:')) {
        base64Data = base64Data.split(',')[1];
      }
      
      // #ifdef APP-PLUS
      // App 端：使用 plus.io 解码
      return plus.io.decodeBase64(base64Data);
      // #endif
      
      // #ifdef H5
      // H5 端：使用 atob 解码
      const byteCharacters = atob(base64Data);
      const byteNumbers = new Array(byteCharacters.length);
      for (let i = 0; i < byteCharacters.length; i++) {
        byteNumbers[i] = byteCharacters.charCodeAt(i);
      }
      return new Uint8Array(byteNumbers).buffer;
      // #endif
    },
    
    /**
     * 辅助方法：写入 Base64 到文件
     */
    writeBase64ToFile(base64Data, fileName) {
      return new Promise((resolve, reject) => {
        plus.io.resolveLocalFileSystemURL('_doc/', (entry) => {
          entry.getFile(fileName, {create: true}, (fileEntry) => {
            fileEntry.createWriter((writer) => {
              // 转换为 ArrayBuffer 写入
              const arrayBuffer = this.base64ToArrayBuffer(base64Data);
              writer.write(arrayBuffer);
              writer.onwrite = () => {
                resolve(fileEntry.toLocalURL());
              };
              writer.onerror = reject;
            }, reject);
          }, reject);
        }, reject);
      });
    },
    
    /**
     * 使用 plus.io 将 Base64 转换为文件（App 端专用）
     */
    async base64ToFileByRenderjs(base64Data) {
      // #ifdef APP-PLUS
      // App 端：使用 plus.io 直接写入
      const fileName = 'upload_' + Date.now() + '.jpg';
      
      console.log('开始保存文件到 _doc 目录');
      
      // 直接使用 plus.io 写入文件
      return new Promise((resolve, reject) => {
        plus.io.resolveLocalFileSystemURL('_doc/', (entry) => {
          console.log('获取 _doc/ 目录成功');
          entry.getFile(fileName, {create: true}, (fileEntry) => {
            console.log('创建文件成功:', fileEntry.fullPath);
            fileEntry.createWriter((writer) => {
              console.log('创建 Writer 成功');
              // 直接写入 Base64 字符串
              let cleanBase64 = base64Data;
              if (cleanBase64.startsWith('data:')) {
                cleanBase64 = cleanBase64.split(',')[1];
              }
              console.log('Base64 长度:', cleanBase64.length);
              
              // 使用 ArrayBuffer 写入
              const arrayBuffer = this.base64ToArrayBuffer(cleanBase64);
              console.log('ArrayBuffer 长度:', arrayBuffer ? arrayBuffer.byteLength : 0);
              
              if (arrayBuffer && arrayBuffer.byteLength > 0) {
                writer.write(arrayBuffer);
                writer.onwrite = () => {
                  console.log('文件写入成功:', fileEntry.toLocalURL());
                  resolve(fileEntry.toLocalURL());
                };
                writer.onerror = (e) => {
                  console.error('文件写入失败:', e);
                  reject(e);
                };
              } else {
                console.error('ArrayBuffer 为空');
                reject(new Error('ArrayBuffer 转换失败'));
              }
            }, reject);
          }, reject);
        }, reject);
      });
      // #endif
      
      // #ifdef H5
      // H5 端：直接返回 Blob
      return this.base64ToBlob(base64Data);
      // #endif
    },
    
    /**
     * 将图片路径转换为 Base64（App 端专用）
     */
    convertImagePathToBase64(imagePath) {
      return new Promise((resolve, reject) => {
        try {
          plus.io.resolveLocalFileSystemURL(imagePath, (entry) => {
            entry.file((file) => {
              const reader = new plus.io.FileReader();
              reader.onload = (e) => {
                resolve(e.target.result);
              };
              reader.onerror = (err) => {
                reject(err);
              };
              reader.readAsDataURL(file);
            }, (err) => {
              reject(err);
            });
          }, (err) => {
            reject(err);
          });
        } catch (e) {
          reject(e);
        }
      });
    },
    
    /**
     * OCR 识别成功处理
     */
    handleOCRSuccess(data) {
      console.log('OCR 识别成功:', data);
      
      // 检查后端返回的状态
      if (data.status === 'error') {
        // 后端返回错误（例如：too many values to unpack）
        console.error('后端 OCR 处理异常:', data.message);
        this.ocrResult = '';
        this.ocrLoading = false;
        
        // 触发错误事件
        this.$emit('ocr-error', new Error(data.message || 'OCR 处理异常'));
        return;
      }
      
      // 正常情况：提取 OCR 结果
      this.ocrResult = data.output || data.ocr_result || '';
      this.ocrLoading = false;
      
      // 触发成功事件
      this.$emit('ocr-success', data);
    },
    
    /**
     * OCR 识别失败处理
     */
    handleOCRError(error) {
      console.error('OCR 识别失败:', error);
      this.ocrResult = '';
      this.ocrLoading = false;
      uni.showToast({
        title: 'OCR 识别失败，请重试',
        icon: 'none'
      });
    }
  }
};
