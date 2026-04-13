<!-- pages/medical/prepare.vue -->
<template>
  <view class="page-container">
    <!-- 顶部导航栏 -->
    <view class="header">
      <view class="padding"></view>
      <view class="header-bottom">
        <view class="header-left">
          <view class="back-btn" @click="goBack">
            <image class="back-icon" src="/static/Register/back.png" mode="aspectFit" />
          </view>
          <text class="page-title">就医准备单</text>
        </view>
        <view class="header-right">
          <image class="icon" src="/static/Mine/export.svg" @click="generatePDF" />
        </view>
      </view>
    </view>

    <!-- 主要内容区域 -->
    <scroll-view class="main-content" scroll-y :show-scrollbar="false" :scroll-x="false">
      <!-- 基本信息 -->
      <view class="basic-info">
        <view class="info-row">
          <image class="info-icon" src="/static/Health/time.svg" mode="aspectFit" />
          <text class="info-label">生成时间：</text>
          <text class="info-value">{{ documentInfo.generatedTime }}</text>
        </view>
        <view class="info-row">
          <image class="info-icon" src="/static/Prepare/department.svg" mode="aspectFit" />
          <text class="info-label">就诊科室：</text>
          <picker mode="selector" :range="departments" :value="departmentIndex" @change="onDepartmentChange">
            <view class="info-value picker-value">
              <text>{{ documentInfo.department }}</text>
              <image class="picker-arrow" src="/static/Health/down.svg" mode="aspectFit" />
            </view>
          </picker>
        </view>
        <view class="info-row">
          <image class="info-icon" src="/static/Login/user.png" mode="aspectFit" />
          <text class="info-label">患者：</text>
          <view class="info-value picker-value" @click="editPatientName">
            <text>{{ documentInfo.patient }}</text>
            <image class="picker-arrow" src="/static/Health/down.svg" mode="aspectFit" />
          </view>
        </view>
        <view class="info-row">
          <image class="info-icon" src="/static/Health/calendar.svg" mode="aspectFit" />
          <text class="info-label">就诊日期：</text>
          <picker mode="date" :value="visitDate" @change="onDateChange">
            <view class="info-value picker-value">
              <text>{{ documentInfo.visitDate }}</text>
              <image class="picker-arrow" src="/static/Health/down.svg" mode="aspectFit" />
            </view>
          </picker>
        </view>
      </view>

      <!-- 近期用药清单 -->
      <view class="section">
        <view class="section-header">
          <view class="section-title-container">
            <image class="section-icon" src="/static/Home/medical-list.svg" mode="aspectFit" />
            <text class="section-title">近期用药清单</text>
          </view>
          <button class="edit-section-btn" @click="editMedications">
            <image class="edit-icon" src="/static/Prepare/edit.svg" mode="aspectFit" />
          </button>
        </view>

        <view class="medication-list">
          <view class="medication-item" v-for="(med, index) in uniqueMedications" :key="index"
            @click="toMedicationDetail(med.id)">
            <view class="medication-header">
              <image class="pill-icon" src="/static/Health/pill-active.svg" mode="aspectFit" />
              <text class="medication-name">{{ med.name }}</text>
            </view>
            <view class="medication-details">
              <view class="detail-item">
                <image class="detail-icon" src="/static/Prepare/schedule.svg" mode="aspectFit" />
                <text class="detail-text">{{ med.schedule }}</text>
              </view>
              <view class="detail-item">
                <image class="detail-icon" src="/static/Health/clock-history.svg" mode="aspectFit" />
                <text class="detail-text">已服用{{ med.takenDays }}天</text>
              </view>
            </view>
          </view>
        </view>

        <view class="section-footer">
          <text class="footer-text">共 {{ uniqueMedications.length }} 种药品</text>
        </view>
      </view>

      <!-- 健康数据情况 -->
      <view class="section">
        <view class="section-header">
          <view class="section-title-container">
            <image class="section-icon" src="/static/Home/warning.svg" mode="aspectFit" />
            <text class="section-title">健康数据情况</text>
          </view>
          <button class="edit-section-btn" @click="editHealthData">
            <image class="edit-icon" src="/static/Prepare/edit.svg" mode="aspectFit" />
          </button>
        </view>

        <!-- 近期健康数据概览 -->
        <view class="health-overview" v-if="healthOverview.length > 0">
          <view class="overview-title">近期健康数据</view>
          <view class="overview-grid">
            <view class="overview-item" v-for="(item, index) in healthOverview" :key="index">
              <view class="overview-icon" :class="item.statusClass">
                <image class="icon-img" :src="getIndicatorIcon(item.type)" mode="aspectFit" />
              </view>
              <view class="overview-info">
                <text class="overview-label">{{ item.indicator }}</text>
                <text class="overview-value" :class="item.statusClass">{{ item.value }} <text class="overview-unit">{{ item.unit }}</text></text>
              </view>
              <view class="overview-status" :class="item.statusClass" v-if="item.isAbnormal">
                <text>{{ item.status }}</text>
              </view>
            </view>
          </view>
        </view>

        <!-- 异常数据提示 -->
        <view class="abnormal-tip" v-if="abnormalData.length > 0">
          <view class="tip-header">
            <image class="tip-icon" src="/static/Home/warning.svg" mode="aspectFit" />
            <text class="tip-title">异常数据提醒</text>
          </view>
          <view class="abnormal-list">
            <view class="abnormal-item" v-for="(item, index) in abnormalData" :key="index">
              <view class="abnormal-left">
                <text class="abnormal-date">{{ item.date }}</text>
                <text class="abnormal-indicator">{{ item.indicator }}: {{ item.value }}{{ item.unit }}</text>
              </view>
              <view class="abnormal-status" :class="item.statusClass">
                <text>{{ item.status }}</text>
              </view>
            </view>
          </view>
        </view>

        <!-- 无数据提示 -->
        <view class="no-data-tip" v-if="healthOverview.length === 0">
          <text>暂无健康数据，请佩戴设备同步</text>
        </view>

        <view class="section-footer">
          <text class="footer-text">最近 7 天健康数据概览</text>
        </view>
      </view>

      <!-- 待咨询问题 -->
      <view class="section">
        <view class="section-header">
          <view class="section-title-container">
            <image class="section-icon" src="/static/Prepare/question.svg" mode="aspectFit" />
            <text class="section-title">待咨询问题</text>
          </view>
          <button class="edit-section-btn" @click="editQuestions">
            <image class="edit-icon" src="/static/Prepare/edit.svg" mode="aspectFit" />
          </button>
        </view>

        <!-- 已选问题列表 -->
        <view class="questions-list" v-if="documentInfo.questions.length > 0">
          <view class="question-item" v-for="(question, index) in documentInfo.questions" :key="index">
            <text class="question-index">•</text>
            <text class="question-text">{{ question }}</text>
            <text class="question-delete" @click="removeQuestion(index)">×</text>
          </view>
        </view>

        <!-- 常用问题推荐 -->
        <view class="common-questions" v-if="!showQuestionInput">
          <view class="common-title">常见问题推荐</view>
          <view class="common-list">
            <view 
              class="common-chip" 
              :class="{ selected: isQuestionSelected(item) }"
              v-for="(item, index) in commonQuestions" 
              :key="index"
              @click="toggleCommonQuestion(item)"
            >
              {{ item }}
            </view>
          </view>
        </view>

        <!-- 添加问题输入框 -->
        <view class="questions-input" v-if="showQuestionInput">
          <textarea class="question-textarea" placeholder="请输入您想问医生的问题..." placeholder-class="textarea-placeholder"
            v-model="newQuestion" maxlength="200" auto-height />
          <view class="textarea-actions">
            <text class="char-count">{{ newQuestion.length }}/200</text>
            <button class="btn-cancel" @click="cancelAddQuestion">取消</button>
            <button class="btn-save" @click="addQuestion" :disabled="!newQuestion.trim()">添加</button>
          </view>
        </view>

        <button class="add-question-btn" @click="showAddQuestion" v-if="!showQuestionInput">
          <image class="add-icon" src="/static/Health/plus-circle.svg" mode="aspectFit" />
          <text class="add-text">自定义问题</text>
        </button>
      </view>

      <!-- 其他信息 -->
      <view class="section">
        <view class="section-header">
          <view class="section-title-container">
            <image class="section-icon" src="/static/DrugScan/note.svg" mode="aspectFit" />
            <text class="section-title">其他信息</text>
          </view>
        </view>

        <view class="other-info">
          <textarea class="other-textarea" placeholder="其他需要告知医生的信息，如过敏史、既往病史等..."
            placeholder-class="textarea-placeholder" v-model="documentInfo.otherInfo" maxlength="500" auto-height />
          <text class="char-count">{{ documentInfo.otherInfo.length }}/500</text>
        </view>
      </view>
    </scroll-view>

    <!-- 底部操作按钮 -->
    <view class="action-buttons">
      <button class="action-btn generate-btn-full" @click="generatePDF">
        <image class="btn-icon" src="/static/Prepare/pdf.svg" mode="aspectFit" />
        <text class="btn-text">生成 PDF</text>
      </button>
    </view>
  </view>
</template>

<script>
import { BASE_URL } from '../../config/config';
import oppoHealthManager from '../../utils/oppoHealthManager';
import reminderApi from '../../api/reminder';

export default {
  data() {
    return {
      refreshing: false,
      departmentIndex: 0,
      visitDate: new Date().toISOString().split('T')[0],
      showQuestionInput: false,
      newQuestion: '',
      departments: ['心内科', '神经内科', '消化内科', '内分泌科', '普通内科', '其他'],
      userId: null,
      healthDataCache: null,
      medicationTasks: [],
      uniqueMedications: [],
      healthOverview: [],
      abnormalData: [],
      commonQuestions: [
        '这个药需要吃多久？',
        '有什么副作用吗？',
        '需要忌口吗？',
        '可以和其他药一起吃吗？',
        '什么时候复查？',
        '症状没有改善怎么办？',
        '这个药饭前还是饭后吃？',
        '忘记吃药了需要补服吗？'
      ],
      documentInfo: {
        generatedTime: '',
        department: '心内科',
        patient: '加载中...',
        visitDate: '',
        medications: [],
        healthData: [],
        questions: [],
        otherInfo: ''
      }
    }
  },
  onLoad(options) {
    this.getUserId();
    if (options.dept) {
      const index = this.departments.findIndex(dept => dept === options.dept)
      if (index !== -1) {
        this.departmentIndex = index
        this.documentInfo.department = this.departments[index]
      }
    }
    this.fetchAllData();
  },
  methods: {
    getUserId() {
      this.userId = uni.getStorageSync('userId') || 1;
      const userInfo = uni.getStorageSync('userInfo');
      if (userInfo) {
        try {
          const user = JSON.parse(userInfo);
          this.documentInfo.patient = user.nickname || user.username || '用户';
        } catch (e) {
          console.error('解析用户信息失败', e);
          this.documentInfo.patient = '用户';
        }
      } else {
        this.documentInfo.patient = '用户';
      }
    },

    async fetchAllData() {
      uni.showLoading({ title: '加载中...' });
      try {
        // 并行获取数据，健康数据可能失败但不影响整体流程
        await Promise.allSettled([
          this.fetchHealthData(),
          this.fetchMedicationTasks()
        ]);
        this.updateDocumentInfo();
      } catch (e) {
        console.error('加载数据异常', e);
      } finally {
        uni.hideLoading();
      }
    },

    async fetchHealthData() {
      try {
        await oppoHealthManager.fetchAllAndCache();
        const fullStr = uni.getStorageSync('OPPO_HEALTH_FULL_DATA');
        if (fullStr) {
          this.healthDataCache = JSON.parse(fullStr);
          console.log('[Prepare] 健康数据加载成功', this.healthDataCache);
        }
      } catch (e) {
        console.warn('[Prepare] 健康数据获取失败（可能是模拟器环境）:', e.message);
        this.healthDataCache = null;
      }
    },

    async fetchMedicationTasks() {
      try {
        const res = await reminderApi.getTodayTasks(this.userId);
        const list = res.data || res || [];
        this.medicationTasks = list.sort((a, b) =>
          a.timePoint.localeCompare(b.timePoint)
        );
        console.log('[Prepare] 用药任务加载成功', this.medicationTasks.length, '条');
      } catch (e) {
        console.error('[Prepare] 获取用药任务失败', e);
        this.medicationTasks = [];
      }
    },

    updateDocumentInfo() {
      const now = new Date();
      this.documentInfo.generatedTime = `${now.getFullYear()}/${now.getMonth() + 1}/${now.getDate()} ${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(2, '0')}`;
      this.documentInfo.visitDate = this.visitDate.replace(/-/g, '/');

      // 转换用药任务数据并去重
      const medications = (this.medicationTasks || []).map(task => {
        return {
          id: task.id,
          name: task.medicineName || '未知药品',
          schedule: `每次${task.dosage || '1 粒'}`,
          takenDays: 7,
          timePoint: task.timePoint
        };
      });

      // 按药品名称去重
      const seen = new Map();
      medications.forEach(med => {
        if (!seen.has(med.name)) {
          seen.set(med.name, med);
        }
      });
      this.uniqueMedications = Array.from(seen.values());

      // 获取健康数据概览
      this.healthOverview = this.getHealthOverview();
      
      // 获取异常数据详情
      this.abnormalData = this.getAbnormalDataDetail();
      
      this.documentInfo.healthData = []; // 保留字段但不再使用
      
      console.log('[Prepare] 文档信息更新完成', {
        uniqueMedications: this.uniqueMedications.length,
        healthOverview: this.healthOverview.length,
        abnormalData: this.abnormalData.length
      });
    },

    getHealthOverview() {
      const data = this.healthDataCache;
      if (!data) {
        console.log('[Prepare] 无健康数据，返回空概览');
        return [];
      }

      const overview = [];
      
      // 计算心率平均值
      const hrData = data.HEART_RATE_COUNT || [];
      if (hrData.length > 0) {
        const validHr = hrData.filter(item => item.average && parseFloat(item.average) > 0);
        if (validHr.length > 0) {
          const avgHr = validHr.reduce((sum, item) => sum + parseFloat(item.average), 0) / validHr.length;
          const isAbnormal = avgHr < 60 || avgHr > 100;
          overview.push({
            type: 'heartRate',
            indicator: '平均心率',
            value: avgHr.toFixed(0),
            unit: 'bpm',
            status: isAbnormal ? (avgHr < 60 ? '偏慢' : '偏快') : '正常',
            statusClass: isAbnormal ? (avgHr < 60 ? 'status-warning' : 'status-danger') : 'status-normal',
            isAbnormal
          });
        }
      }

      // 计算血压平均值
      const bpData = data.BLOOD_PRESSURE_COUNT || [];
      if (bpData.length > 0) {
        const validBp = bpData.filter(item => item.blood_pressure_systolic_max && parseInt(item.blood_pressure_systolic_max) > 0);
        if (validBp.length > 0) {
          const avgSys = validBp.reduce((sum, item) => parseInt(item.blood_pressure_systolic_max), 0) / validBp.length;
          const avgDia = validBp.reduce((sum, item) => parseInt(item.blood_pressure_diastolic_min || 0), 0) / validBp.length;
          const isAbnormal = avgSys > 130 || avgDia > 85;
          overview.push({
            type: 'bloodPressure',
            indicator: '平均血压',
            value: `${avgSys.toFixed(0)}/${avgDia.toFixed(0)}`,
            unit: 'mmHg',
            status: isAbnormal ? '偏高' : '正常',
            statusClass: isAbnormal ? 'status-danger' : 'status-normal',
            isAbnormal
          });
        }
      }

      // 计算血氧平均值
      const oxyData = data.BLOOD_OXYGEN_COUNT || [];
      if (oxyData.length > 0) {
        const validOxy = oxyData.filter(item => item.blood_oxygen_min && parseFloat(item.blood_oxygen_min) > 0);
        if (validOxy.length > 0) {
          const avgOxy = validOxy.reduce((sum, item) => parseFloat(item.blood_oxygen_min), 0) / validOxy.length;
          const isAbnormal = avgOxy < 95;
          overview.push({
            type: 'bloodOxygen',
            indicator: '平均血氧',
            value: avgOxy.toFixed(1),
            unit: '%',
            status: isAbnormal ? (avgOxy >= 90 ? '偏低' : '极低') : '正常',
            statusClass: isAbnormal ? (avgOxy >= 90 ? 'status-warning' : 'status-danger') : 'status-normal',
            isAbnormal
          });
        }
      }

      // 添加睡眠数据
      const sleepData = data.SLEEP_COUNT || [];
      if (sleepData.length > 0 && sleepData[0].total) {
        const duration = (sleepData[0].total / 3600).toFixed(1);
        const score = sleepData[0].sleep_score || 0;
        const isAbnormal = duration < 6 || duration > 9 || score < 60;
        overview.push({
          type: 'sleep',
          indicator: '昨晚睡眠',
          value: duration,
          unit: '小时',
          status: isAbnormal ? '不足' : '充足',
          statusClass: isAbnormal ? 'status-warning' : 'status-normal',
          isAbnormal
        });
      }

      console.log('[Prepare] 健康数据概览:', overview);
      return overview;
    },

    getAbnormalDataDetail() {
      const data = this.healthDataCache;
      if (!data) {
        console.log('[Prepare] 无健康数据，返回空异常数据');
        return [];
      }

      const abnormalData = [];
      const d = new Date();
      const formatDate = (date) => `${date.getMonth() + 1}/${date.getDate()}`;

      for (let i = 0; i < 7; i++) {
        const dateStr = d.toISOString().split('T')[0];
        
        // 检查心率数据
        const hrData = data.HEART_RATE_COUNT?.find(item => 
          item.day?.startsWith(dateStr)
        );
        if (hrData && hrData.average) {
          const hr = parseFloat(hrData.average);
          if (hr < 60 || hr > 100) {
            abnormalData.push({
              id: `hr-${i}`,
              date: formatDate(d),
              type: 'heartRate',
              indicator: '心率',
              value: hr.toFixed(0),
              unit: 'bpm',
              status: hr < 60 ? '偏慢' : '偏快',
              statusClass: hr < 60 ? 'status-warning' : 'status-danger'
            });
          }
        }

        // 检查血压数据
        const bpData = data.BLOOD_PRESSURE_COUNT?.find(item => 
          item.day?.startsWith(dateStr)
        );
        if (bpData && bpData.blood_pressure_systolic_max) {
          const sys = parseInt(bpData.blood_pressure_systolic_max);
          const dia = parseInt(bpData.blood_pressure_diastolic_min || 0);
          if (sys > 130 || dia > 85) {
            abnormalData.push({
              id: `bp-${i}`,
              date: formatDate(d),
              type: 'bloodPressure',
              indicator: '血压',
              value: `${sys}/${dia}`,
              unit: 'mmHg',
              status: '偏高',
              statusClass: 'status-danger'
            });
          }
        }

        // 检查血氧数据
        const oxyData = data.BLOOD_OXYGEN_COUNT?.find(item => 
          item.day?.startsWith(dateStr)
        );
        if (oxyData && oxyData.blood_oxygen_min) {
          const oxy = parseFloat(oxyData.blood_oxygen_min);
          if (oxy < 95 && oxy > 0) {
            abnormalData.push({
              id: `oxy-${i}`,
              date: formatDate(d),
              type: 'bloodOxygen',
              indicator: '血氧',
              value: oxy.toFixed(1),
              unit: '%',
              status: oxy >= 90 ? '偏低' : '极低',
              statusClass: oxy >= 90 ? 'status-warning' : 'status-danger'
            });
          }
        }

        d.setDate(d.getDate() - 1);
      }

      console.log('[Prepare] 异常健康数据:', abnormalData.length, '条');
      return abnormalData;
    },
    onRefresh() {
      this.refreshing = true
      this.fetchAllData().finally(() => {
        this.refreshing = false
        uni.showToast({
          title: '已更新',
          icon: 'success'
        })
      })
    },
    goBack() {
      uni.navigateBack()
    },
    editPatientName() {
      uni.showModal({
        title: '填写患者姓名',
        editable: true,
        placeholderText: '请输入您的真实姓名',
        defaultText: this.documentInfo.patient === '用户' ? '' : this.documentInfo.patient,
        success: (res) => {
          if (res.confirm && res.content) {
            this.documentInfo.patient = res.content.trim();
            const userInfo = uni.getStorageSync('userInfo');
            let user = {};
            if (userInfo) {
              try {
                user = JSON.parse(userInfo);
              } catch (e) {
                console.error('解析用户信息失败', e);
              }
            }
            user.nickname = res.content.trim();
            uni.setStorageSync('userInfo', JSON.stringify(user));
            uni.showToast({
              title: '保存成功',
              icon: 'success'
            });
          }
        }
      });
    },
    onDepartmentChange(e) {
      this.departmentIndex = e.detail.value
      this.documentInfo.department = this.departments[this.departmentIndex]
    },
    onDateChange(e) {
      this.visitDate = e.detail.value
      // 格式化日期
      const date = new Date(e.detail.value)
      this.documentInfo.visitDate = `${date.getFullYear()}/${date.getMonth() + 1}/${date.getDate()}`
    },
    editMedications() {
      uni.navigateTo({
        url: '/pages/medical/edit-medications'
      })
    },
    editHealthData() {
      uni.navigateTo({
        url: '/pages/medical/edit-health-data'
      })
    },
    editQuestions() {
      uni.showActionSheet({
        itemList: ['编辑问题', '清空问题'],
        success: (res) => {
          if (res.tapIndex === 0) {
            this.showAddQuestion()
          } else if (res.tapIndex === 1) {
            this.clearQuestions()
          }
        }
      })
    },
    showAddQuestion() {
      this.showQuestionInput = true
      this.newQuestion = ''
    },
    addQuestion() {
      if (this.newQuestion.trim()) {
        this.documentInfo.questions.push(this.newQuestion.trim())
        this.newQuestion = ''
        this.showQuestionInput = false
        uni.showToast({
          title: '问题已添加',
          icon: 'success'
        })
      }
    },

    toggleCommonQuestion(question) {
      const index = this.documentInfo.questions.indexOf(question);
      if (index > -1) {
        // 已选中则取消
        this.documentInfo.questions.splice(index, 1);
      } else {
        // 未选中则添加
        this.documentInfo.questions.push(question);
      }
      uni.showToast({
        title: index > -1 ? '已取消' : '已添加',
        icon: 'none'
      });
    },

    isQuestionSelected(question) {
      return this.documentInfo.questions.includes(question);
    },

    removeQuestion(index) {
      this.documentInfo.questions.splice(index, 1);
      uni.showToast({
        title: '已删除',
        icon: 'success'
      });
    },
    cancelAddQuestion() {
      this.showQuestionInput = false
      this.newQuestion = ''
    },
    clearQuestions() {
      uni.showModal({
        title: '清空问题',
        content: '确定要清空所有待咨询问题吗？',
        success: (res) => {
          if (res.confirm) {
            this.documentInfo.questions = []
            uni.showToast({
              title: '已清空',
              icon: 'success'
            })
          }
        }
      })
    },
    toMedicationDetail(id) {
      uni.navigateTo({
        url: `/pages/medication/detail?id=${id}`
      })
    },
    getIndicatorIcon(type) {
      const icons = {
        bloodPressure: '/static/Prepare/blood-pressure.svg',
        heartRate: '/static/Home/heart.svg',
        bloodSugar: '/static/Prepare/blood-sugar.svg',
        weight: '/static/Prepare/weight.svg'
      }
      return icons[type] || '/static/Prepare/health.svg'
    },
    editContent() {
      uni.navigateTo({
        url: '/pages/medical/edit-document'
      })
    },
    shareDocument() {
      uni.showActionSheet({
        itemList: ['分享给微信好友', '分享到朋友圈', '复制链接', '生成分享图'],
        success: (res) => {
          const actions = [
            this.shareToWechat,
            this.shareToTimeline,
            this.copyLink,
            this.generateShareImage
          ]
          if (actions[res.tapIndex]) {
            actions[res.tapIndex]()
          }
        }
      })
    },
    shareToWechat() {
      uni.showToast({
        title: '准备分享到微信...',
        icon: 'none'
      })
    },
    shareToTimeline() {
      uni.showToast({
        title: '准备分享到朋友圈...',
        icon: 'none'
      })
    },
    copyLink() {
      uni.setClipboardData({
        data: 'https://medical-prepare.com/share/123456',
        success: () => {
          uni.showToast({
            title: '链接已复制',
            icon: 'success'
          })
        }
      })
    },
    generateShareImage() {
      uni.showLoading({
        title: '生成分享图中...'
      })

      setTimeout(() => {
        uni.hideLoading()
        uni.showToast({
          title: '分享图已生成',
          icon: 'success'
        })
      }, 1500)
    },
    exportDocument() {
      this.generatePDF()
    },
    async generatePDF() {
      uni.showLoading({ title: '生成 PDF 中...' })
      try {
        const accessToken = uni.getStorageSync('accessToken') || ''
        const payload = {
          generatedTime: this.documentInfo.generatedTime || '',
          department: this.documentInfo.department || '',
          patient: this.documentInfo.patient || '',
          visitDate: this.documentInfo.visitDate || '',
          medications: (this.documentInfo.medications || []).map(med => ({
            id: med.id,
            name: med.name,
            schedule: med.schedule,
            takenDays: med.takenDays,
            missedCount: med.missedCount,
            status: med.status
          })),
          healthData: (this.documentInfo.healthData || []).map(data => ({
            id: data.id,
            date: data.date,
            indicator: data.indicator,
            value: data.value,
            unit: data.unit,
            status: data.status
          })),
          questions: this.documentInfo.questions || [],
          otherInfo: this.documentInfo.otherInfo || ''
        }

        const res = await uni.request({
          url: `${BASE_URL}/medical/prepare/pdf`,
          method: 'POST',
          data: payload,
          timeout: 15000,
          header: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${accessToken}`
          }
        })

        const body = res?.data || {}
        const success = body.success === true || body.code === 0
        const fileUrl = body.fileUrl || body?.data?.fileUrl

        console.log('[PDF] resp=', body)
        console.log('[PDF] fileUrl=', fileUrl)

        if (!success || !fileUrl) {
          uni.showModal({
            title: '生成失败',
            content: body.message || '未返回文件地址',
            showCancel: false
          })
          return
        }

        // #ifdef H5
        // H5 环境：直接在新窗口打开（不需要认证，因为后端已配置公开访问）
        window.open(fileUrl, '_blank')
        uni.showToast({ title: 'PDF 已打开', icon: 'success' })
        // #endif

        // #ifndef H5
        // 小程序/App 环境：使用 downloadFile 下载
        // 注意：downloadFile 的 header 参数需要确保 token 正确
        uni.downloadFile({
          url: fileUrl,
          header: {
            'Authorization': `Bearer ${accessToken}`
          },
          timeout: 20000,
          success: (downloadRes) => {
            console.log('[PDF] download success', downloadRes.statusCode, downloadRes.tempFilePath)
            if (downloadRes.statusCode === 200) {
              // 下载成功，打开 PDF
              uni.openDocument({
                filePath: downloadRes.tempFilePath,
                showMenu: true,
                success: () => {
                  console.log('[PDF] openDocument success')
                },
                fail: (openErr) => {
                  console.error('[PDF] openDocument fail', openErr)
                  uni.showModal({
                    title: '打开失败',
                    content: '文件已下载，但无法打开',
                    showCancel: false
                  })
                }
              })
            } else if (downloadRes.statusCode === 402 || downloadRes.statusCode === 401) {
              // Token 过期或无效
              console.error('[PDF] download fail: 认证失败')
              uni.showModal({
                title: '认证失败',
                content: '请重新登录后重试',
                showCancel: false,
                success: () => {
                  // 跳转到登录页
                  uni.reLaunch({ url: '/pages/login/Login' })
                }
              })
            } else {
              // 其他错误
              console.error('[PDF] download fail: HTTP', downloadRes.statusCode)
              uni.showModal({
                title: '下载失败',
                content: `HTTP ${downloadRes.statusCode}`,
                showCancel: false
              })
            }
          },
          fail: (err) => {
            console.error('[PDF] download fail', err)
            uni.showModal({
              title: '下载失败',
              content: err.errMsg || '网络错误',
              showCancel: false
            })
          }
        })
        // #endif
      } catch (e) {
        console.error('[PDF] generate error=', e)
        uni.showModal({
          title: '生成失败',
          content: e?.message || e?.errMsg || '请求异常',
          showCancel: false
        })
      } finally {
        uni.hideLoading()
      }
    }

  }
}
</script>

<style scoped lang="scss">
.page-container {
  min-height: 100vh;
  background: linear-gradient(180deg, #f8faff 0%, #ffffff 100%);
  padding-bottom: 200rpx;
}

/* 头部导航栏 */
.header {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: space-between;
  padding: 60rpx 32rpx 24rpx;
  background: linear-gradient(135deg, #4d8eff 0%, #2d6bff 100%);
  border-radius: 0 0 32rpx 32rpx;
  box-shadow: 0 4rpx 20rpx rgba(45, 107, 255, 0.15);
}

.padding {
  height: 64rpx;
  /* 顶部留白，适配状态栏 */
}

.header-bottom {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
}

.header-left {
  display: flex;
  align-items: center;
  flex: 1;
}

.back-btn {
  width: 64rpx;
  height: 64rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 50%;
  margin-right: 20rpx;
  transition: all 0.3s ease;
}

.back-btn:active {
  background: rgba(255, 255, 255, 0.3);
}

.back-icon {
  width: 32rpx;
  height: 32rpx;
}

.page-title {
  font-size: 40rpx;
  font-weight: 700;
  color: #ffffff;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 20rpx;
}

.icon {
  width: 48rpx;
  height: 48rpx;
}

.action-btn {
  display: flex;
  align-items: center;
  justify-content: center;

  height: 64rpx;
  width: 64rpx;
  background: transparent;
}

.share-icon,
.export-icon {
  width: 48rpx;
  height: 48rpx;
}

/* 主要内容区域 */
.main-content {
  height: 100%;
  padding: 32rpx;
  box-sizing: border-box;
  overflow-x: hidden !important;
}

.main-content::-webkit-scrollbar {
  display: none;
}

/* 基本信息 */
.basic-info {
  background: #ffffff;
  border-radius: 24rpx;
  padding: 32rpx;
  margin-bottom: 32rpx;
  box-shadow: 0 6rpx 24rpx rgba(45, 107, 255, 0.08);
  border: 2rpx solid rgba(77, 142, 255, 0.1);
}

.info-row {
  display: flex;
  align-items: center;
  margin-bottom: 24rpx;

  &:last-child {
    margin-bottom: 0;
  }
}

.info-icon {
  width: 32rpx;
  height: 32rpx;
  margin-right: 16rpx;
  flex-shrink: 0;
}

.info-label {
  font-size: 28rpx;
  color: #87909c;
  min-width: 140rpx;
}

.info-value {
  font-size: 28rpx;
  font-weight: 600;
  color: #2d3b4e;
  flex: 1;
}

.picker-value {
  flex: 1;
  max-width: 200rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12rpx 20rpx;
  background: rgba(77, 142, 255, 0.1);
  border-radius: 16rpx;
  transition: all 0.3s ease;

  &:active {
    background: rgba(77, 142, 255, 0.2);
  }
}

.picker-arrow {
  width: 20rpx;
  height: 20rpx;
  margin-left: 8rpx;
  flex-shrink: 0;
}

/* 分区样式 */
.section {
  background: #ffffff;
  border-radius: 24rpx;
  padding: 32rpx;
  margin-bottom: 32rpx;
  box-shadow: 0 6rpx 24rpx rgba(45, 107, 255, 0.08);
  border: 2rpx solid rgba(77, 142, 255, 0.1);
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 28rpx;
}

.section-title-container {
  display: flex;
  align-items: center;
}

.section-icon {
  width: 36rpx;
  height: 36rpx;
  margin-right: 16rpx;
}

.section-title {
  font-size: 32rpx;
  font-weight: 700;
  color: #2d3b4e;
}

.edit-section-btn {
  display: flex;
  align-items: center;
  justify-content: center;

  height: 64rpx;
  width: 64rpx;

  padding: 0;

  margin-left: auto;
  margin-right: 0;
  background: rgba(77, 142, 255, 0.1);
  border-radius: 32rpx;
  border: #2d6bff solid 2rpx;
}

.edit-icon {
  width: 48rpx;
  height: 48rpx;

  background-position: center;
  background-repeat: no-repeat;
  background-size: cover;
}

.edit-text {
  font-size: 24rpx;
  color: #4d8eff;
  font-weight: 500;
}

/* 药品清单 */
.medication-list {
  display: flex;
  flex-direction: column;
  gap: 24rpx;
  margin-bottom: 24rpx;
}

.medication-item {
  padding: 24rpx;
  background: rgba(77, 142, 255, 0.05);
  border-radius: 20rpx;
  border: 2rpx solid rgba(77, 142, 255, 0.1);
  transition: all 0.3s ease;

  &:active {
    background: rgba(77, 142, 255, 0.1);
  }
}

.medication-header {
  display: flex;
  align-items: center;
  margin-bottom: 16rpx;
}

.pill-icon {
  width: 32rpx;
  height: 32rpx;
  margin-right: 16rpx;
}

.medication-name {
  font-size: 30rpx;
  font-weight: 600;
  color: #2d3b4e;
  flex: 1;
}

.medication-details {
  padding-left: 48rpx;
}

.detail-item {
  display: flex;
  align-items: center;
  margin-bottom: 12rpx;

  &:last-child {
    margin-bottom: 0;
  }
}

.detail-icon {
  width: 24rpx;
  height: 24rpx;
  margin-right: 12rpx;
}

.detail-text {
  font-size: 26rpx;
  color: #555e6d;
  flex: 1;
}

.missed-count {
  margin-left: 16rpx;
}

.missed-text {
  font-size: 22rpx;
  color: #ff6b6b;
  font-weight: 500;
}

/* 健康数据 */
/* 健康数据 */
.health-overview {
  margin-bottom: 24rpx;
  padding: 24rpx;
  background: rgba(77, 142, 255, 0.05);
  border-radius: 20rpx;
  border: 2rpx solid rgba(77, 142, 255, 0.1);
}

.overview-title {
  font-size: 28rpx;
  font-weight: 600;
  color: #2d3b4e;
  margin-bottom: 20rpx;
}

.overview-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20rpx;
}

.overview-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 20rpx;
  background: #fff;
  border-radius: 16rpx;
  box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.05);
}

.overview-icon {
  width: 64rpx;
  height: 64rpx;
  border-radius: 32rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 12rpx;

  &.status-normal {
    background: rgba(16, 185, 129, 0.1);
  }

  &.status-warning {
    background: rgba(245, 158, 11, 0.1);
  }

  &.status-danger {
    background: rgba(255, 107, 107, 0.1);
  }

  .icon-img {
    width: 32rpx;
    height: 32rpx;
  }
}

.overview-info {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 8rpx;
}

.overview-label {
  font-size: 24rpx;
  color: #87909c;
  margin-bottom: 4rpx;
}

.overview-value {
  font-size: 32rpx;
  font-weight: 700;
  color: #2d3b4e;

  &.status-normal {
    color: #10b981;
  }

  &.status-warning {
    color: #f59e0b;
  }

  &.status-danger {
    color: #ff6b6b;
  }

  .overview-unit {
    font-size: 20rpx;
    font-weight: 400;
    color: #87909c;
  }
}

.overview-status {
  padding: 4rpx 12rpx;
  border-radius: 12rpx;
  font-size: 20rpx;
  font-weight: 500;

  &.status-normal {
    background: rgba(16, 185, 129, 0.1);
    color: #10b981;
  }

  &.status-warning {
    background: rgba(245, 158, 11, 0.1);
    color: #f59e0b;
  }

  &.status-danger {
    background: rgba(255, 107, 107, 0.1);
    color: #ff6b6b;
  }
}

/* 异常数据提醒 */
.abnormal-tip {
  padding: 24rpx;
  background: rgba(255, 107, 107, 0.05);
  border-radius: 20rpx;
  border: 2rpx solid rgba(255, 107, 107, 0.1);
  margin-bottom: 24rpx;
}

.tip-header {
  display: flex;
  align-items: center;
  margin-bottom: 16rpx;
}

.tip-icon {
  width: 32rpx;
  height: 32rpx;
  margin-right: 12rpx;
}

.tip-title {
  font-size: 28rpx;
  font-weight: 600;
  color: #ff6b6b;
}

.abnormal-list {
  display: flex;
  flex-direction: column;
  gap: 12rpx;
}

.abnormal-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16rpx;
  background: #fff;
  border-radius: 12rpx;
}

.abnormal-left {
  display: flex;
  flex-direction: column;
  gap: 4rpx;
}

.abnormal-date {
  font-size: 22rpx;
  color: #87909c;
  font-weight: 500;
}

.abnormal-indicator {
  font-size: 26rpx;
  color: #2d3b4e;
  font-weight: 600;
}

.abnormal-status {
  padding: 6rpx 16rpx;
  border-radius: 16rpx;
  font-size: 22rpx;
  font-weight: 500;

  &.status-warning {
    background: rgba(245, 158, 11, 0.1);
    color: #f59e0b;
  }

  &.status-danger {
    background: rgba(255, 107, 107, 0.1);
    color: #ff6b6b;
  }
}

/* 无数据提示 */
.no-data-tip {
  padding: 40rpx;
  text-align: center;
  color: #87909c;
  font-size: 26rpx;
}

.health-data-list {
  display: flex;
  flex-direction: column;
  gap: 20rpx;
  margin-bottom: 24rpx;
}

.health-data-item {
  padding: 24rpx;
  background: rgba(245, 158, 11, 0.05);
  border-radius: 20rpx;
  border: 2rpx solid rgba(245, 158, 11, 0.1);
}

.data-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12rpx;
}

.data-date {
  font-size: 26rpx;
  font-weight: 600;
  color: #f59e0b;
}

.data-indicator {
  display: flex;
  align-items: center;
}

.indicator-icon {
  width: 24rpx;
  height: 24rpx;
  margin-right: 8rpx;
}

.indicator-name {
  font-size: 26rpx;
  color: #555e6d;
}

.data-value {
  display: flex;
  align-items: baseline;
}

.value-number {
  font-size: 36rpx;
  font-weight: 700;
  color: #2d3b4e;
  margin-right: 8rpx;
}

.value-unit {
  font-size: 24rpx;
  color: #87909c;
  margin-right: 20rpx;
}

.data-status {
  padding: 6rpx 16rpx;
  border-radius: 16rpx;
  font-size: 22rpx;
  font-weight: 500;

  &.status-warning {
    background: rgba(245, 158, 11, 0.1);
    color: #f59e0b;
  }

  &.status-danger {
    background: rgba(255, 107, 107, 0.1);
    color: #ff6b6b;
  }
}

/* 待咨询问题 */
.questions-list {
  display: flex;
  flex-direction: column;
  gap: 24rpx;
  margin-bottom: 24rpx;
}

.question-item {
  display: flex;
  align-items: flex-start;
  padding: 24rpx;
  background: rgba(77, 142, 255, 0.05);
  border-radius: 20rpx;
  border: 2rpx solid rgba(77, 142, 255, 0.1);
  margin-bottom: 16rpx;
  position: relative;
}

.question-index {
  font-size: 36rpx;
  color: #4d8eff;
  margin-right: 20rpx;
  line-height: 1;
  flex-shrink: 0;
}

.question-text {
  font-size: 28rpx;
  color: #2d3b4e;
  line-height: 1.4;
  flex: 1;
}

.question-delete {
  font-size: 40rpx;
  color: #ff6b6b;
  line-height: 1;
  padding: 0 8rpx;
  flex-shrink: 0;
  transition: all 0.2s;

  &:active {
    opacity: 0.6;
    transform: scale(0.9);
  }
}

/* 常见问题推荐 */
.common-questions {
  margin-bottom: 24rpx;
  padding: 24rpx;
  background: rgba(16, 185, 129, 0.05);
  border-radius: 20rpx;
  border: 2rpx solid rgba(16, 185, 129, 0.1);
}

.common-title {
  font-size: 26rpx;
  font-weight: 600;
  color: #10b981;
  margin-bottom: 16rpx;
}

.common-list {
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx;
}

.common-chip {
  padding: 12rpx 24rpx;
  background: rgba(77, 142, 255, 0.1);
  border: 2rpx solid rgba(77, 142, 255, 0.2);
  border-radius: 24rpx;
  font-size: 24rpx;
  color: #4d8eff;
  transition: all 0.3s;

  &.selected {
    background: rgba(16, 185, 129, 0.15);
    border-color: rgba(16, 185, 129, 0.3);
    color: #10b981;
  }

  &:active {
    opacity: 0.7;
    transform: scale(0.98);
  }
}

.questions-input {
  margin-bottom: 24rpx;
  background: rgba(77, 142, 255, 0.05);
  border-radius: 20rpx;
  border: 2rpx solid rgba(77, 142, 255, 0.2);
  overflow: hidden;
}

.question-textarea {
  width: 100%;
  min-height: 160rpx;
  padding: 24rpx;
  font-size: 28rpx;
  color: #2d3b4e;
  line-height: 1.4;
  background: transparent;
}

.textarea-placeholder {
  color: #b4bfd3;
  font-size: 28rpx;
}

.textarea-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16rpx 24rpx;
  border-top: 2rpx solid rgba(77, 142, 255, 0.1);
}

.char-count {
  font-size: 24rpx;
  color: #87909c;
}

.cancel-btn,
.btn-cancel,
.add-btn,
.btn-save {
  padding: 12rpx 24rpx;
  border-radius: 16rpx;
  border: none;
  font-size: 26rpx;
  font-weight: 500;
  margin-left: 16rpx;
  transition: all 0.3s ease;
}

.cancel-btn,
.btn-cancel {
  background: rgba(180, 191, 211, 0.1);
  color: #87909c;

  &:active {
    background: rgba(180, 191, 211, 0.2);
  }
}

.add-btn,
.btn-save {
  background: rgba(77, 142, 255, 0.1);
  color: #4d8eff;

  &:active {
    background: rgba(77, 142, 255, 0.2);
  }

  &[disabled] {
    background: rgba(180, 191, 211, 0.1);
    color: #b4bfd3;
    opacity: 0.6;
  }
}

.add-question-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  padding: 28rpx 0;
  background: linear-gradient(135deg, rgba(77, 142, 255, 0.1) 0%, rgba(45, 107, 255, 0.05) 100%);
  border: 2rpx dashed rgba(77, 142, 255, 0.3);
  border-radius: 20rpx;
  transition: all 0.3s ease;

  &:active {
    background: linear-gradient(135deg, rgba(77, 142, 255, 0.15) 0%, rgba(45, 107, 255, 0.1) 100%);
  }
}

.add-icon {
  width: 32rpx;
  height: 32rpx;
  margin-right: 12rpx;
}

.add-text {
  font-size: 28rpx;
  color: #4d8eff;
  font-weight: 600;
}

/* 其他信息 */
.other-info {
  background: rgba(77, 142, 255, 0.05);
  border-radius: 20rpx;
  border: 2rpx solid rgba(77, 142, 255, 0.1);
  overflow: hidden;
}

.other-textarea {
  width: 100%;
  min-height: 200rpx;
  padding: 24rpx;
  font-size: 28rpx;
  color: #2d3b4e;
  line-height: 1.4;
  background: transparent;
}

/* 分区页脚 */
.section-footer {
  padding-top: 16rpx;
  border-top: 2rpx solid rgba(77, 142, 255, 0.1);
}

.footer-text {
  font-size: 24rpx;
  color: #87909c;
  text-align: center;
  display: block;
}

/* 底部操作按钮 */
.action-buttons {
  position: fixed;
  bottom: 120rpx;
  left: 0;
  right: 0;
  display: flex;
  padding: 0 32rpx;
  gap: 24rpx;
  z-index: 100;
}

.action-btn {
  flex: 1;
  height: 96rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0;
  font-size: 28rpx;
  font-weight: 600;
  transition: all 0.3s ease;

  &:active {
    transform: scale(0.98);
  }
}

.generate-btn-full {
  flex: 1;
  max-width: 600rpx;
  margin: 0 auto;
  background: linear-gradient(135deg, #4d8eff 0%, #2d6bff 100%);
  border-radius: 48rpx;
  box-shadow: 0 8rpx 24rpx rgba(77, 142, 255, 0.3);

  .btn-icon {
    width: 40rpx;
    height: 40rpx;
    margin-right: 16rpx;
  }

  .btn-text {
    color: #ffffff;
    font-size: 32rpx;
  }
}

.edit-btn {
  background: linear-gradient(135deg, #ffffff 0%, #f8faff 100%);
  border: 2rpx solid rgba(77, 142, 255, 0.2);
  box-shadow: 0 4rpx 16rpx rgba(45, 107, 255, 0.1);

  .btn-icon {
    width: 32rpx;
    height: 32rpx;
    margin-right: 12rpx;
  }

  .btn-text {
    color: #4d8eff;
  }
}

.generate-btn {
  background: linear-gradient(135deg, #4d8eff 0%, #2d6bff 100%);
  box-shadow: 0 8rpx 32rpx rgba(45, 107, 255, 0.3);

  .btn-icon {
    width: 32rpx;
    height: 32rpx;
    margin-right: 12rpx;
  }

  .btn-text {
    color: #ffffff;
  }
}
</style>