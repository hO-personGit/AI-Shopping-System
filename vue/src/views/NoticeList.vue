<template>
    <div class="notice-manager">
        <!-- 页面标题区域 -->
        <div class="page-header">
            <h2>通知管理</h2>
            <el-tag type="info">共 {{ total }} 条通知</el-tag>
        </div>

        <div class="content-box">
            <!-- 操作栏 -->
            <el-card class="operation-area" shadow="hover">
                <!-- 搜索区域 -->
                <el-form ref="searchForm" :inline="true" :model="listQuery" class="search-form">
                    <el-form-item label="通知标题">
                        <el-input v-model="listQuery.title" placeholder="请输入标题" clearable></el-input>
                    </el-form-item>
   
                    <el-form-item>
                        <el-button size="medium" type="primary" plain @click="onSubmit">
                            <i class="el-icon-search"></i> 查询
                        </el-button>
                        <el-button size="medium" plain @click="onReset">
                            <i class="el-icon-refresh"></i> 重置
                        </el-button>
                    </el-form-item>
                </el-form>

                <!-- 按钮区域 -->
                <div class="control-btns">
                    <el-button  type="danger" plain size="medium" @click="batchDelete" :disabled="!multipleSelection.length">
                        <i class="el-icon-delete"></i> 批量删除
                    </el-button>
                    <div class="right-btns">
                        <el-button type="primary" plain size="medium" @click="openDialog()">
                            <i class="el-icon-plus"></i> 发布通知
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
                         style="width: 100%" 
                         size="medium" 
                         @selection-change="handleSelectionChange"
                         border>
                    <el-table-column type="selection" width="50" align="center"></el-table-column>
                    <el-table-column prop="id" label="编号" width="80" align="center"></el-table-column>
                    <el-table-column prop="title" label="标题" min-width="150"></el-table-column>
                    <el-table-column label="标签" width="200">
                        <template slot-scope="scope">
                            <span>{{ formatTags(scope.row.tags) }}</span>
                        </template>
                    </el-table-column>
                    <el-table-column prop="content" label="内容" min-width="200" show-overflow-tooltip></el-table-column>
                    <el-table-column prop="time" label="发布时间" width="160" align="center"></el-table-column>
                    <el-table-column label="操作" width="180" align="center" fixed="right">
                        <template slot-scope="scope">
                            <el-button size="mini" type="primary" plain @click="openDialog(scope.row)">
                                <i class="el-icon-edit"></i> 编辑
                            </el-button>
                            <el-button size="mini" type="danger" plain @click="handleDelete(scope.$index, scope.row)">
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
            <el-dialog :title="dialogForm.id ? '编辑通知' : '发布通知'" 
                      :visible.sync="formVisible" 
                      width="500px"
                      :close-on-click-modal="false">
                <el-form :model="dialogForm" :rules="formRules" ref="dialogForm" label-width="100px">
                    <el-form-item label="通知标题" prop="title">
                        <el-input v-model="dialogForm.title" placeholder="请输入通知标题"></el-input>
                    </el-form-item>
                    <el-form-item label="通知内容" prop="content">
                        <el-input type="textarea" 
                                 v-model="dialogForm.content" 
                                 :autosize="{ minRows: 4, maxRows: 8 }" 
                                 placeholder="请输入通知内容"
                                 maxlength="500"
                                 show-word-limit>
                        </el-input>
                    </el-form-item>
                    <el-form-item label="标签" prop="tags">
                        <div class="tags-input-container">
                            <el-tag v-for="tag in dialogForm.tags"
                                   :key="tag"
                                   closable
                                   @close="removeFormTag(tag)"
                                   style="margin-right: 6px; margin-bottom: 6px">
                                {{tag}}
                            </el-tag>
                            <el-input v-if="dialogForm.tags.length < 3"
                                     v-model="dialogForm.tagInputValue"
                                     size="small"
                                     style="width: 100px"
                                     @keyup.enter.native="handleFormTagInput"
                                     @blur="handleFormTagInput"
                                     placeholder="+ 添加标签">
                            </el-input>
                        </div>
                        <div class="tags-tip" style="font-size: 12px; color: #909399; margin-top: 5px">
                            最多添加3个标签，按回车或失去焦点时添加
                        </div>
                    </el-form-item>
                </el-form>
                <div slot="footer" class="dialog-footer">
                    <el-button @click="formVisible = false" plain>取 消</el-button>
                    <el-button type="primary" :loading="isSubmit" @click="handleSave('dialogForm')" plain>确 定</el-button>
                </div>
            </el-dialog>
        </div>
    </div>
</template>

<script>
import Pagination from '../components/Pagination/index.vue'
import Request from '../utils/request.js'

export default {
    name: 'NoticeList',
    components: { Pagination },
    data() {
        return {
            urlPrefix: '/notice/', // URL 前缀
            pageTitle: '通知',
            listLoading: true,
            listQuery: {
                title: '',
                tags: [],
                tagInputValue: '',
                currentPage: 1,
                pageSize: 10
            },
            dialogForm: {
                id: undefined,
                title: '',
                content: '',
                tags: [],
                tagInputValue: ''
            },
            total: 0,
            tableData: [],
            multipleSelection: [],
            formVisible: false,
            tagOptions: ['重要', '紧急', '普通', '公告'],
            formRules: {
                title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
                content: [{ required: true, message: '请输入内容', trigger: 'blur' }],
                tags: [{ required: true, message: '请至少添加一个标签', trigger: 'change' }]
            },
            isSubmit: false
        }
    },
    created() {
        this.fetchData()
    },
    methods: {
        handleSelectionChange(val) {
            this.multipleSelection = val.map(v => v.id)
        },
        openDialog(row = {}) {
            // 确保 tags 是数组
            const tags = row.tags ? (Array.isArray(row.tags) ? row.tags : row.tags.split(',').filter(Boolean)) : [];
            
            this.dialogForm = {
                ...row,
                tagInputValue: '',
                tags: tags
            };
            this.formVisible = true;
        },
        handleDelete(index, row) {
            this.$confirm('此操作将删除选中数据, 是否继续?', '提示', {
                confirmButtonText: '确定',
                cancelButtonText: '取消',
                type: 'warning'
            }).then(() => {
                Request.delete(this.urlPrefix + row.id).then(response => {
                    if (response.code == 0) {
                        this.$message({
                            type: 'success',
                            message: '删除成功!'
                        })
                        this.onReset()
                    } else {
                        this.$message({
                            type: 'error',
                            message: response.msg
                        })
                    }
                });
            }).catch(() => {
                this.$message({
                    type: 'info',
                    message: '已取消删除'
                })
            })
        },
        batchDelete() {
            if (this.multipleSelection.length < 1) {
                this.$message({
                    message: '请先选择要删除的数据！',
                    type: 'warning'
                })
            } else {
                Request.delete(`${this.urlPrefix}deleteBatch?ids=${this.multipleSelection.join(',')}`).then(res => {
                    if (res.code === '0') {
                        this.$message({
                            showClose: true,
                            message: '批量删除成功',
                            type: 'success'
                        });
                        this.onReset()
                    } else {
                        this.$message({
                            showClose: true,
                            message: res.msg,
                            type: 'error',
                        });
                    }
                })
            }
        },
        fetchData() {
            this.listLoading = true
            Request.get(`${this.urlPrefix}page`, {
                params: {
                    title: this.listQuery.title,
                    tags: this.listQuery.tags.join(','),
                    currentPage: this.listQuery.currentPage,
                    size: this.listQuery.pageSize,
                }
            }).then(response => {
                const data = response.data;
                if (response.code === '0') {
                    this.total = data.total
                    this.tableData = data.records.map(record => ({
                        ...record,
                        tags: record.tags ? record.tags.split(',').filter(Boolean) : []
                    }));
                }
            })
            this.listLoading = false
        },
        onSubmit() {
            this.listQuery.currentPage = 1
            this.fetchData()
        },
        onReset() {
            this.listQuery.currentPage = 1
            this.listQuery.pageSize = 10;
            this.listQuery.title = '';
            this.listQuery.tags = [];
            this.listQuery.tagInputValue = '';
            this.fetchData()
        },
        handleSave(formName) {
            this.$refs[formName].validate(valid => {
                if (valid) {
                    const formData = {
                        ...this.dialogForm,
                        tags: this.dialogForm.tags.join(',')
                    };
                    const requestMethod = formData.id ? Request.put : Request.post;
                    const url = formData.id ? `${this.urlPrefix}${formData.id}` : "/notice";
                    requestMethod(url, formData).then(response => {
                        if (response.code == 0) {
                            this.$message({
                                showClose: true,
                                message: this.dialogForm.id ? '更新成功' : '添加成功',
                                type: 'success',
                            });
                            this.fetchData()
                            this.formVisible = false
                        } else {
                            this.$message({
                                showClose: true,
                                message: response.msg,
                                type: 'error',
                            });
                        }
                    });
                }
            });
        },
        formatTags(tags) {
            if (!tags || !Array.isArray(tags)) return '';
            return tags.slice(0, 3).join('，');
        },
        handleSearchTagInput() {
            const tagValue = this.listQuery.tagInputValue.trim();
            if (tagValue) {
                if (this.listQuery.tags.length >= 3) {
                    this.$message({
                        message: '最多只能添加3个标签',
                        type: 'warning'
                    });
                    return;
                }
                if (!this.listQuery.tags.includes(tagValue)) {
                    this.listQuery.tags.push(tagValue);
                }
                this.listQuery.tagInputValue = '';
            }
        },
        removeSearchTag(tag) {
            const index = this.listQuery.tags.indexOf(tag);
            if (index > -1) {
                this.listQuery.tags.splice(index, 1);
            }
        },
        handleFormTagInput() {
            const tagValue = this.dialogForm.tagInputValue.trim();
            if (tagValue) {
                if (this.dialogForm.tags.length >= 3) {
                    this.$message({
                        message: '最多只能添加3个标签',
                        type: 'warning'
                    });
                    return;
                }
                if (!this.dialogForm.tags.includes(tagValue)) {
                    this.dialogForm.tags.push(tagValue);
                }
                this.dialogForm.tagInputValue = '';
            }
        },
        removeFormTag(tag) {
            const index = this.dialogForm.tags.indexOf(tag);
            if (index > -1) {
                this.dialogForm.tags.splice(index, 1);
            }
        },
    }
}
</script>

<style lang="less" scoped>
.notice-manager {
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
    background-color: #fafafa;
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
</style>