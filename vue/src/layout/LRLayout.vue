<template>
    <div class="auth-container">
        <div class="bg-image"></div>
        <div class="bg-overlay"></div>
        
        <div class="auth-content">
            <div class="auth-card">
                <div class="auth-card-content">
                    <router-view />
                </div>
            </div>
        </div>
    </div>
</template>

<script>
export default {
    name: 'LRLayout'
}
</script>

<style scoped>
.auth-container {
    min-height: 100vh;
    display: flex;
    flex-direction: column;
    position: relative;
    overflow: hidden;
}

.bg-image {
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background-image: url('@/assets/imgs/2.jpg');
    background-size: cover;
    background-position: center;
    z-index: 0;
}

.bg-overlay {
    display: none; /* 隐藏遮罩层 */
}

.auth-content {
    flex: 1;
    display: flex;
    justify-content: center;
    align-items: center;
    padding: 30px 24px;
    position: relative;
    z-index: 1;
    transition: all 0.8s ease;
}

.auth-card {
    display: flex;
    width: 480px;
    height: auto;
    min-height: 500px;
    max-height: 90vh;
    border-radius: 5px;
    overflow: hidden;
    box-shadow: none;
    background-color: white;
    position: relative;
    transition: none;
    backdrop-filter: none;
}

.auth-card:hover {
    box-shadow: none;
}

.auth-card-content {
    width: 100%;
    height: 100%;
    padding: 20px 30px;
    display: flex;
    justify-content: center;
    align-items: center;
    background: transparent;
    border-radius: 5px;
    overflow-y: auto;
}

@media (max-width: 576px) {
    .auth-content {
        padding: 24px 16px;
    }
    
    .auth-card-content {
        padding: 30px 20px;
    }
}
</style>