<template>
    <div class="icon-manager">
        <!-- 页面标题区域 -->
        <div class="page-header">
            <h2>图标管理</h2>
            <el-tag type="info">共 {{ total }} 个图标</el-tag>
        </div>

        <div class="content-box">
            <!-- 操作栏 -->
            <el-card class="operation-area" shadow="hover">
                <!-- 搜索区域 -->
                <el-form ref="searchForm" :inline="true" :model="listQuery" class="search-form">
                    <el-form-item label="图标名称">
                        <el-input v-model="listQuery.itemKey" placeholder="请输入图标名称" clearable></el-input>
                    </el-form-item>
                    <el-form-item>
                        <el-button type="primary" plain @click="onSubmit">
                            <i class="el-icon-search"></i> 查询
                        </el-button>
                        <el-button plain @click="onReset">
                            <i class="el-icon-refresh"></i> 重置
                        </el-button>
                    </el-form-item>
                </el-form>

                <!-- 按钮区域 -->
                <div class="control-btns">
                    <el-button type="danger" plain size="medium" @click="batchDelete" :disabled="!multipleSelection.length">
                        <i class="el-icon-delete"></i> 批量删除
                    </el-button>
                    <div class="right-btns">
                        <el-button type="primary" plain size="medium" @click="exportVisible = true">
                            <i class="el-icon-download"></i> 导出数据
                        </el-button>
                        <el-button type="primary" plain size="medium" @click="handleCreate">
                            <i class="el-icon-plus"></i> 新增图标
                        </el-button>
                    </div>
                </div>
            </el-card>

            <!-- 表格区域 -->
            <el-card class="table-card" shadow="hover">
                <el-table ref="multipleTable" 
                         v-loading="listLoading" 
                         :data="tableData" 
                         tooltip-effect="dark" 
                         row-key="id"
                         style="width: 100%" 
                         size="medium" 
                         @selection-change="handleSelectionChange"
                         border>
                    <el-table-column type="selection" width="50" align="center"></el-table-column>
                    <el-table-column prop="id" label="ID" width="80" align="center"></el-table-column>
                    <el-table-column prop="itemKey" label="图标名称" min-width="120"></el-table-column>
                    <el-table-column prop="itemValue" label="图标值" min-width="120"></el-table-column>
                    <el-table-column label="图标预览" width="100" align="center">
                        <template slot-scope="scope">
                            <i :class="scope.row.itemValue" style="font-size: 20px;"></i>
                        </template>
                    </el-table-column>
                    <el-table-column prop="description" label="描述" min-width="150"></el-table-column>
                    <el-table-column prop="createTime" label="创建时间" width="160" align="center"></el-table-column>
                    <el-table-column prop="updateTime" label="更新时间" width="160" align="center"></el-table-column>
                    <el-table-column label="操作" width="180" align="center" fixed="right">
                        <template slot-scope="scope">
                            <el-button size="mini" type="primary" plain @click="handleEdit(scope.row)">
                                <i class="el-icon-edit"></i> 编辑
                            </el-button>
                            <el-button size="mini" type="danger" plain @click="handleDelete(scope.row.id)">
                                <i class="el-icon-delete"></i> 删除
                            </el-button>
                        </template>
                    </el-table-column>
                </el-table>

                <!-- 分页 -->
                <pagination v-show="total > 0" 
                          :total="total" 
                          :page.sync="listQuery.currentPage" 
                          :limit.sync="listQuery.pageSize" 
                          @pagination="fetchData" />
            </el-card>

            <!-- 新增/编辑弹窗 -->
            <el-dialog :title="dialogForm.id ? '编辑图标' : '新增图标'" 
                      :visible.sync="formVisible" 
                      width="500px"
                      :close-on-click-modal="false">
                <el-form :model="dialogForm" :rules="formRules" ref="dialogForm" label-width="100px">
                    <el-form-item label="图标名称" prop="itemKey">
                        <el-input v-model="dialogForm.itemKey" placeholder="请输入图标名称"></el-input>
                    </el-form-item>
                    <el-form-item label="图标值" prop="itemValue">
                        <el-input v-model="dialogForm.itemValue" placeholder="请输入图标值">
                            <template slot="append">
                                <i :class="dialogForm.itemValue" style="font-size: 18px;"></i>
                            </template>
                        </el-input>
                    </el-form-item>
                    <el-form-item label="描述">
                        <el-input type="textarea" v-model="dialogForm.description" placeholder="请输入描述信息"></el-input>
                    </el-form-item>
                </el-form>
                <div slot="footer" class="dialog-footer">
                    <el-button @click="formVisible = false" plain>取 消</el-button>
                    <el-button type="primary" :loading="isSubmit" @click="handleSave('dialogForm')" plain>确 定</el-button>
                </div>
            </el-dialog>

            <!-- 导出数据弹窗 -->
            <el-dialog title="导出数据" :visible.sync="exportVisible" width="400px" :close-on-click-modal="false">
                <div class="export-data">
                    <el-button type="primary" plain @click="exportTable('xlsx')">
                        <i class="el-icon-document"></i> EXCEL格式
                    </el-button>
                    <el-button type="primary" plain @click="exportTable('csv')">
                        <i class="el-icon-document"></i> CSV格式
                    </el-button>
                    <el-button type="primary" plain @click="exportTable('txt')">
                        <i class="el-icon-document"></i> TXT格式
                    </el-button>
                </div>
                <div class="hints">提示：请选择要导出数据的格式</div>
            </el-dialog>
        </div>
    </div>
</template>

<script>
import excel from '../utils/excel.js'
import Pagination from '../components/Pagination/index.vue'
import Upload from '../components/Upload/index.vue'
import Hints from '../components/Hints/index.vue'
import Request from '../utils/request.js'
export default {
    name: 'IconDict',
    components: { Pagination, Upload, Hints },
    data() {
        return {
            // 数据列表加载动画
            listLoading: true,
            formLabelWidth: '80px',

            // 查询列表参数对象
            listQuery: {
                itemKey: undefined,
                currentPage: 1,
                pageSize: 10
            },
            // 新增/编辑提交表单对象
            dialogForm: {
                id: undefined, // 需要有id字段以区分新增和编辑
                dictTypeCode: 'icon', // 字典类型Code，这里假设是固定的
                itemKey: '',
                itemValue: '',
                description: '',
            },

            // 数据总条数
            total: 0,
            // 表格数据数组
            tableData: [],
            // 多选数据暂存数组
            multipleSelection: [],
            // 新增/编辑 弹出框显示/隐藏
            formVisible: false,
            // 表单校验 trigger: ['blur', 'change'] 为多个事件触发
            formRules: {
                itemKey: [
                    { required: true, message: '字典键不能为空', trigger: 'blur' },
                    { min: 1, max: 20, message: '字典键长度必须在1到20个字符之间', trigger: 'blur' }
                ],
                itemValue: [
                    { required: true, message: '字典值不能为空', trigger: 'blur' },
                    { min: 1, max: 50, message: '字典值长度必须在1到50个字符之间', trigger: 'blur' }
                ],

                description: [
                    { max: 100, message: '描述内容不能超过100个字符', trigger: 'blur' }
                ],

            },
            // 防止多次连续提交表单
            isSubmit: false,

            // 导出文件格式
            filesFormat: '.txt, .csv, .xls, .xlsx',
            // 导出数据 弹出框显示/隐藏
            exportVisible: false
        }
    },
    created() {
        this.fetchData()
    },
    methods: {
        // 多选操作
        handleSelectionChange(val) {
            this.multipleSelection = val.map(v => v.id);
            console.log('选中的数据:', this.multipleSelection);
        },
        // 新建数据
        handleCreate() {
            this.dialogForm = {
                dictTypeCode: 'icon',
                itemKey: '',
                itemValue: '',
                description: ''
            }
            this.formVisible = true
        },
        // 编辑数据
        handleEdit(row) {
            this.dialogForm = JSON.parse(JSON.stringify(row))
            this.formVisible = true
        },
        cancel() {
            this.$message.success('取消操作成功');
        },
        // 删除数据
        handleDelete(id) {
            this.$confirm('确认删除该图标?', '提示', {
                confirmButtonText: '确定',
                cancelButtonText: '取消',
                type: 'warning'
            }).then(async () => {
                try {
                    const res = await Request.delete(`/dictitem/deleteById/${id}`);
                    if (res.code === '0') {
                        this.$message.success('删除成功');
                        this.fetchData();
                    } else {
                        this.$message.error(res.msg || '删除失败');
                    }
                } catch (error) {
                    console.error('删除失败:', error);
                    this.$message.error('删除失败');
                }
            }).catch(() => {
                this.$message.info('已取消删除');
            });
        },

        // 批量删除
        batchDelete() {
            if (!this.multipleSelection.length) {
                this.$message.warning('请先选择要删除的数据！');
                return;
            }

            this.$confirm('确认删除选中的图标?', '提示', {
                confirmButtonText: '确定',
                cancelButtonText: '取消',
                type: 'warning'
            }).then(async () => {
                try {
                    const ids = this.multipleSelection.join(',');
                    const res = await Request.delete(`/dictitem/deleteBatch?idList=${ids}`);
                    if (res.code === '0') {
                        this.$message.success('批量删除成功');
                        this.fetchData();
                    } else {
                        this.$message.error(res.msg || '批量删除失败');
                    }
                } catch (error) {
                    console.error('批量删除失败:', error);
                    this.$message.error('批量删除失败');
                }
            }).catch(() => {
                this.$message.info('已取消删除');
            });
        },

        // 获取数据列表
        async fetchData() {
            this.listLoading = true;
            try {
                const res = await Request.get("/dictitem/findPage", {
                    params: {
                        code: "icon",
                        itemKey: this.listQuery.itemKey,
                        currentPage: this.listQuery.currentPage,
                        size: this.listQuery.pageSize,
                    }
                });
                
                if (res.code === '0') {
                    this.total = res.data.total;
                    this.tableData = res.data.records;
                    // 清空选择
                    this.multipleSelection = [];
                    if (this.$refs.multipleTable) {
                        this.$refs.multipleTable.clearSelection();
                    }
                }
            } catch (error) {
                console.error('获取数据失败:', error);
                this.$message.error('获取数据失败');
            } finally {
                this.listLoading = false;
            }
        },
        // 查询数据
        onSubmit() {
            this.listQuery.currentPage = 1
            this.fetchData()
        },
        onReset() {
            this.listQuery.currentPage = 1
            this.listQuery.pageSize = 10;
            this.listQuery.itemKey = "";
            this.fetchData()
        },

        handleSave(formName) {
            this.$refs[formName].validate(async valid => {
                if (!valid) {
                    console.log('表单验证失败!')
                    return false
                }

                this.isSubmit = true
                try {
                    // 创建一个新对象，只包含需要的字段
                    const submitData = {
                        dictTypeCode: this.dialogForm.dictTypeCode,
                        itemKey: this.dialogForm.itemKey,
                        itemValue: this.dialogForm.itemValue,
                        description: this.dialogForm.description
                    }
                    
                    // 如果是编辑，添加 id
                    if (this.dialogForm.id) {
                        submitData.id = this.dialogForm.id
                    }

                    const method = submitData.id ? 'put' : 'post'
                    const url = submitData.id 
                        ? `/dictitem/${submitData.id}` 
                        : '/dictitem/save'
                    
                    const response = await Request[method](url, submitData)
                    
                    if (response.code === '0') {
                        this.$message.success(submitData.id ? '更新成功' : '添加成功')
                        this.formVisible = false
                        this.fetchData()
                    } else {
                        this.$message.error(response.msg || (submitData.id ? '更新失败' : '添加失败'))
                    }
                } catch (error) {
                    console.error('保存失败:', error)
                    this.$message.error('操作失败')
                } finally {
                    this.isSubmit = false
                }
            })
        },

        // 导出数据--excle格式
        exportTable(type) {
            if (this.tableData.length) {
                const params = {
                    header: ['菜单ID', '菜单名', '菜单路径', '菜单图标', '描述', '父级菜单ID', '页面路径', '排序', '菜单所属角色'], // 修改表头以匹配类属性
                    key: ['id', 'name', 'path', 'icon', 'description', 'pid', 'pagePath', 'sortNum', 'role'], // 修改key以匹配类属性
                    data: this.tableData, // 假定this.tableData已经是格式化好的数组，每个元素是一个菜单对象
                    autoWidth: true,
                    fileName: '菜单数据表',
                    bookType: type
                }
                excel.exportDataToExcel(params)
                this.exportVisible = false
            }
        }
    }
}
</script>

<style lang="less" scoped>
.icon-manager {
    padding: 24px;
    background-color: #f0f2f5;
    min-height: calc(100vh - 60px);
}

.page-header {
    display: flex;
    align-items: center;
    margin-bottom: 24px;

    h2 {
        font-size: 24px;
        font-weight: 500;
        color: #1f2f3d;
        margin: 0;
        margin-right: 15px;
    }
}

.operation-area {
    background: white;
    border-radius: 8px;
    box-shadow: 0 1px 4px rgba(0, 0, 0, 0.05);
    margin-bottom: 24px;
}

.search-form {
    margin-bottom: 20px;
    padding: 20px 20px 0;
    border-radius: 4px;
}

.table-card {
    border-radius: 8px;
    box-shadow: 0 1px 4px rgba(0, 0, 0, 0.05);
}

.control-btns {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 0 20px 20px;

    .right-btns {
        display: flex;
        gap: 10px;
    }
}

:deep(.el-table) {
    border-radius: 8px;
    overflow: hidden;
    box-shadow: none;

    th {
        background-color: #fafafa;
        font-weight: 500;
        color: #1f2f3d;
        padding: 12px 0;
    }

    td {
        padding: 12px 0;
    }
}

:deep(.el-dialog) {
    border-radius: 12px;
    overflow: hidden;
    box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);

    .el-dialog__header {
        padding: 24px;
        border-bottom: 1px solid #ebeef5;
        margin: 0;
    }

    .el-dialog__title {
        font-size: 18px;
        font-weight: 500;
        color: #1f2f3d;
    }

    .el-dialog__body {
        padding: 32px 24px;
    }

    .el-dialog__footer {
        padding: 16px 24px;
        border-top: 1px solid #ebeef5;
    }
}

.export-data {
    display: flex;
    justify-content: space-around;
    margin-bottom: 20px;
}

.hints {
    text-align: center;
    color: #909399;
    font-size: 12px;
}
</style>