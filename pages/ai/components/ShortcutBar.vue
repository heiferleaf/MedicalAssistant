<template>
	<scroll-view scroll-x class="shortcut-bar" show-scrollbar="false">
		<view 
			v-for="(shortcut, index) in shortcuts" 
			:key="index" 
			class="shortcut-item"
			@click="handleClick(shortcut)"
		>
			<image v-if="shortcut.icon" class="icon" :src="shortcut.icon"/>
			<text class="text">{{ shortcut.text }}</text>
		</view>
	</scroll-view>
</template>

<script>
export default {
	name: 'ShortcutBar',
	props: {
		shortcuts: {
			type: Array,
			default: () => []
		}
	},
	methods: {
		handleClick(shortcut) {
			this.$emit('click', shortcut);
		}
	}
}
</script>

<style lang="scss" scoped>
.shortcut-bar {
	white-space: nowrap; 
	padding: 0 30rpx 24rpx;
	
	.shortcut-item {
		display: inline-flex; 
		align-items: center; 
		gap: 8rpx; 
		padding: 12rpx 24rpx;
		background: #fff; 
		border: 1rpx solid #e2e8f0; 
		border-radius: 100rpx; 
		margin-right: 16rpx;
		font-size: 24rpx; 
		font-weight: 600; 
		color: #475569;
		cursor: pointer;
		transition: all 0.2s;
		
		&:active {
			transform: scale(0.95);
		}
		
		@media (prefers-color-scheme: dark) { 
			background: #1e293b; 
			border-color: #334155; 
			color: #f1f5f9; 
		}
		
		.icon {
			width: 28rpx;
			height: 28rpx;
		}
	}
}
</style>
