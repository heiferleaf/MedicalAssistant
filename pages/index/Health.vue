<template>
	<view class="container" :class="{ 'dark-mode': isDarkMode }" :style="{ '--base-font': globalFontSize + 'px' }">
		<view class="header">
			<text class="title">我的药箱</text>
			<button class="add-btn" type="primary" size="mini" @click="openForm('add')">新增药品</button>
		</view>

		<view class="list" v-if="medicineList.length > 0">
			<view class="card" v-for="item in medicineList" :key="item.id">
				<view class="card-header">
					<text class="med-name">{{ item.name }}</text>
					<text class="med-dosage">推荐剂量: {{ item.defaultDosage || '暂无' }}</text>
				</view>
				<view class="card-body">
					<text class="med-remark">备注: {{ item.remark || '无' }}</text>
				</view>
				<view class="card-actions">
					<button size="mini" @click="openPlanForm(item)">生成计划</button>
					<button size="mini" @click="openForm('edit', item)">编辑</button>
					<button size="mini" type="warn" @click="handleDelete(item.id)">删除</button>
				</view>
			</view>
		</view>

		<view class="empty" v-else>
			<text>药箱空空如也，快去添加药品吧~</text>
		</view>

		<view class="modal-mask" v-if="showForm">
			<view class="modal-content">
				<text class="modal-title">{{ formType === 'add' ? '新增药品' : '编辑药品' }}</text>
				<input class="input" v-model="formData.name" placeholder="请输入药品名称(必填)" />
				<input class="input" v-model="formData.defaultDosage" placeholder="推荐剂量 (如: 2粒)" />
				<input class="input" v-model="formData.remark" placeholder="备注 (如: 饭后服用)" />

				<view class="modal-actions">
					<button size="mini" @click="showForm = false">取消</button>
					<button size="mini" type="primary" @click="submitForm">保存</button>
				</view>
			</view>
		</view>

		<view class="modal-mask" v-if="showPlanForm">
			<view class="modal-content">
				<text class="modal-title">创建用药计划</text>
				<input class="input" v-model="planFormData.startDate" placeholder="开始日期 (YYYY-MM-DD)" />
				<input class="input" v-model="planFormData.endDate" placeholder="结束日期 (可选, YYYY-MM-DD)" />
				<input class="input" v-model="planFormData.dosage" placeholder="本次剂量 (可选)" />
				<input class="input" v-model="planTimeInput" placeholder="服药时间 (如 08:00,20:00，逗号分隔)" />
				<input class="input" v-model="planFormData.remark" placeholder="备注 (可选)" />

				<view class="modal-actions">
					<button size="mini" @click="showPlanForm = false">取消</button>
					<button size="mini" type="primary" @click="submitPlan">创建</button>
				</view>
			</view>
		</view>
	</view>
</template>

<script>
// 引入您刚刚封装的 API
// ⚠️ 请确保这里的相对路径指向您项目中的实际位置
import medicineApi from '../../api/medicine.js'

export default {
	data() {
		return {
			globalFontSize: 16, // 默认基准字体大小 16px
			isDarkMode: false,
			medicineList: [],

			showForm: false,
			formType: 'add',
			formData: {
				id: null,
				name: '',
				defaultDosage: '',
				remark: ''
			},

			showPlanForm: false,
			currentMedId: null,
			planTimeInput: '',
			planFormData: {
				dosage: '',
				startDate: '',
				endDate: '',
				timePoints: [],
				remark: ''
			}
		}
	},
	// 2. 使用 onShow 替代 mounted，这样从设置页返回时能立即刷新字体
	onShow() {
		this.applyGlobalSettings();
	},
	mounted() {
		this.fetchMedicineList();
		this.applyGlobalSettings();
	},
	methods: {
		// 读取全局设置
		applyGlobalSettings() {
			const savedFont = uni.getStorageSync('app_font_size');
			console.log('读取到的全局字体大小:', savedFont); // 调试输出，查看读取到的值
			if (savedFont) {
				this.globalFontSize = savedFont;
			}
			const savedTheme = uni.getStorageSync('app_theme');
      this.isDarkMode = (savedTheme === 'dark');
		},
		// 1. 获取药品列表
		async fetchMedicineList() {
			try {
				// uni.showLoading({ title: '加载中...' })
				const res = await medicineApi.getMedicineList()
				// 假设您的 httpRequest 返回了完整的响应体 { code, data, message }
				console.log('获取药品列表响应:', res) // 调试输出，查看 API 返回的完整响应
				if (res) {
					this.medicineList = res || []
				} else {
					uni.showToast({ title: res.message || '获取列表失败', icon: 'none' })
				}
			} catch (error) {
				uni.showToast({ title: '网络异常', icon: 'none' })
			} finally {
				// uni.hideLoading()
			}
		},

		// 2. 打开新增/编辑弹窗
		openForm(type, item = null) {
			this.formType = type
			if (type === 'edit' && item) {
				// 回显数据
				this.formData = {
					id: item.id,
					name: item.name,
					defaultDosage: item.defaultDosage || '',
					remark: item.remark || ''
				}
			} else {
				// 清空表单
				this.formData = { id: null, name: '', defaultDosage: '', remark: '' }
			}
			this.showForm = true
		},

		// 3. 提交新增/编辑药品
		async submitForm() {
			if (!this.formData.name) {
				return uni.showToast({ title: '药品名称不能为空', icon: 'none' })
			}

			try {
				uni.showLoading({ title: '保存中...' })
				let res;
				const payload = {
					name: this.formData.name,
					defaultDosage: this.formData.defaultDosage,
					remark: this.formData.remark
				}

				if (this.formType === 'add') {
					res = await medicineApi.addMedicine(payload)
				} else {
					res = await medicineApi.editMedicine(this.formData.id, payload)
				}
				console.log('提交表单响应:', res) // 调试输出，查看 API 返回的完整响应

				if (res) {
					uni.showToast({ title: '保存成功', icon: 'success' })
					this.showForm = false
					this.fetchMedicineList() // 刷新列表
				} else {
					uni.showToast({ title: res.message || '操作失败', icon: 'none' })
				}
			} catch (error) {
				console.error('提交表单失败:', error)
				uni.showToast({ title: '网络异常', icon: 'none' })
			} finally {
				uni.hideLoading()
			}
		},

		// 4. 删除药品
		handleDelete(id) {
			uni.showModal({
				title: '危险操作',
				content: '确定要删除这个药品吗？',
				success: async (confirmRes) => {
					if (confirmRes.confirm) {
						try {
							uni.showLoading({ title: '删除中...' })
							const res = await medicineApi.deleteMedicine(id)
							if (res && res.code === 200) {
								uni.showToast({ title: '删除成功', icon: 'success' })
								this.fetchMedicineList() // 重新获取最新列表
							} else {
								uni.showToast({ title: res.message || '删除失败', icon: 'none' })
							}
						} catch (error) {
							uni.showToast({ title: '网络异常', icon: 'none' })
						} finally {
							uni.hideLoading()
						}
					}
				}
			})
		},

		// 5. 打开创建计划弹窗
		openPlanForm(item) {
			this.currentMedId = item.id
			this.planTimeInput = '08:00,20:00' // 给用户一个默认的输入示例

			// 自动填入今天的日期作为开始日期
			const today = new Date().toISOString().split('T')[0]
			this.planFormData = {
				dosage: item.defaultDosage || '',
				startDate: today,
				endDate: '',
				timePoints: [],
				remark: item.remark || ''
			}
			this.showPlanForm = true
		},

		// 6. 提交用药计划
		async submitPlan() {
			if (!this.planFormData.startDate) {
				return uni.showToast({ title: '开始日期不能为空', icon: 'none' })
			}
			if (!this.planTimeInput) {
				return uni.showToast({ title: '服药时间不能为空', icon: 'none' })
			}

			// 文本处理：将 "08:00, 20:00" 转换为 API 需要的 ["08:00", "20:00"]
			this.planFormData.timePoints = this.planTimeInput
				.split(',')
				.map(t => t.trim())
				.filter(t => t !== '')

			try {
				uni.showLoading({ title: '创建计划中...' })
				const res = await medicineApi.createMedicinePlan(this.currentMedId, this.planFormData)

				if (res && res.code === 200) {
					uni.showToast({ title: '计划创建成功', icon: 'success' })
					this.showPlanForm = false
				} else {
					uni.showToast({ title: res.message || '操作失败', icon: 'none' })
				}
			} catch (error) {
				uni.showToast({ title: '网络异常', icon: 'none' })
			} finally {
				uni.hideLoading()
			}
		}
	}
}
</script>

<style scoped>
/* 3. 在 CSS 中，通过 calc(var(--base-font)) 替换固定的像素值 */

.container {
  padding: 20px;
  background-color: #f5f5f5;
  min-height: 100vh;
  /* 可以在这里设置一个兜底，防止变量失效 */
  font-size: var(--base-font, 16px); 
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.title {
  /* 原来是 20px，比基准大 4px */
  font-size: calc(var(--base-font) + 4px);
  font-weight: bold;
}

.card {
  background-color: #ffffff;
  border-radius: 8px;
  padding: 15px;
  margin-bottom: 15px;
  box-shadow: 0 2px 5px rgba(0,0,0,0.05);
}

.card-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 10px;
}

.med-name {
  /* 原来是 18px，比基准大 2px */
  font-size: calc(var(--base-font) + 2px);
  font-weight: bold;
  color: #333;
}

.med-dosage, .med-remark {
  /* 原来是 14px，比基准小 2px */
  font-size: calc(var(--base-font) - 2px);
  color: #666;
}

.card-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 15px;
  border-top: 1px solid #eee;
  padding-top: 10px;
}

.empty {
  text-align: center;
  color: #999;
  margin-top: 50px;
  /* 继承容器字体大小 */
}

/* 简易弹窗遮罩和内容样式 */
.modal-mask {
  position: fixed;
  top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0,0,0,0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 999;
}

.modal-content {
  width: 80%;
  background: #fff;
  border-radius: 10px;
  padding: 20px;
}

.modal-title {
  /* 原来是 18px，比基准大 2px */
  font-size: calc(var(--base-font) + 2px);
  font-weight: bold;
  margin-bottom: 15px;
  display: block;
  text-align: center;
}

.input {
  border: 1px solid #eee;
  border-radius: 4px;
  padding: 10px;
  margin-bottom: 15px;
  /* 原来是 14px，比基准小 2px */
  font-size: calc(var(--base-font) - 2px);
  background-color: #fafafa;
}

.modal-actions {
  display: flex;
  justify-content: space-around;
  margin-top: 10px;
}

/* ================== 暗黑模式覆盖样式 ================== */
.dark-mode.container {
  background-color: #0f172a; /* 页面深色底色 */
}

/* 标题类文字变白 */
.dark-mode .title,
.dark-mode .med-name,
.dark-mode .modal-title {
  color: #f1f5f9; 
}

/* 次要文字变浅灰 */
.dark-mode .med-dosage, 
.dark-mode .med-remark,
.dark-mode .empty {
  color: #94a3b8; 
}

/* 卡片和弹窗背景变深色 */
.dark-mode .card,
.dark-mode .modal-content {
  background-color: #1e293b; 
  box-shadow: 0 2px 8px rgba(0,0,0,0.5);
}

/* 边框和线条变暗 */
.dark-mode .card-actions {
  border-top-color: #334155; 
}

/* 输入框适配 */
.dark-mode .input {
  background-color: #0f172a; 
  border-color: #334155;
  color: #f1f5f9;
}
</style>