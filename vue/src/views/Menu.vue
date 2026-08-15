<template>
    <div class="menu-manager">
        <!-- 页面标题区域 -->
        <div class="page-header">
            <h2>菜单管理</h2>
            <el-tag type="info">共 {{ tableData.length }} 个菜单项</el-tag>
        </div>

        <div class="content-box">
            <!-- 操作栏 -->
            <el-card class="operation-area" shadow="hover">
                <div class="control-btns">
                    <div class="right-btns">
                        <el-button type="primary" plain size="medium" @click="exportVisible = true">
                            <i class="el-icon-download"></i> 导出数据
                        </el-button>
                        <el-button type="primary" plain size="medium" @click="openDialog('add')">
                            <i class="el-icon-plus"></i> 新增菜单项
                        </el-button>
                    </div>
                </div>
            </el-card>

            <!-- 表格区域 -->
            <el-card class="table-card" shadow="hover">
                <el-table ref="multipleTable" v-loading="listLoading" :data="tableData"
                     row-key="id" tooltip-effect="dark" style="width: 100%"
                     size="medium" border default-expand-all>
                    <el-table-column prop="id" label="ID" width="80" align="center"></el-table-column>
                    <el-table-column prop="name" label="菜单名称" min-width="120"></el-table-column>
                    <el-table-column prop="path" label="菜单路径" min-width="120"></el-table-column>
                    <el-table-column label="菜单图标" width="100" align="center">
                        <template slot-scope="scope">
                            <i :class="scope.row.icon" style="font-size: 18px;"></i>
                        </template>
                    </el-table-column>
                    <el-table-column prop="description" label="菜单描述" min-width="150"></el-table-column>
                    <el-table-column prop="pagePath" label="页面路径" min-width="120"></el-table-column>
                    <el-table-column prop="role" label="菜单分配" min-width="120">
                        <template slot-scope="scope">
                            <el-tag v-if="scope.row.role.includes('SUPER_ADMIN')" size="small" type="danger">超级管理员</el-tag>
                            <el-tag v-if="scope.row.role.includes('ADMIN')&&scope.row.role!='SUPER_ADMIN'" size="small" type="warning">管理员</el-tag>
                            <el-tag v-if="scope.row.role.includes('MERCHANT')" size="small" type="success">商户</el-tag>
                            <el-tag v-if="scope.row.role.includes('USER')" size="small" type="info">普通用户</el-tag>
                        </template>
                    </el-table-column>
                    <el-table-column prop="sortNum" label="排序" width="80" align="center"></el-table-column>
                    <el-table-column label="操作" width="350" align="center" fixed="right">
                        <template slot-scope="scope">
                            <el-button v-if="!scope.row.pid && !scope.row.path" 
                                     size="mini" type="success" plain
                                     @click="addChildMenu(scope.row.id)">
                                <i class="el-icon-plus"></i> 子菜单
                            </el-button>
                            <el-button size="mini" type="primary" plain @click="openDialog('edit', scope.row)">
                                <i class="el-icon-edit"></i> 编辑
                            </el-button>
                            <el-button size="mini" type="danger" plain @click="handleDelete(scope.$index, scope.row)">
                                <i class="el-icon-delete"></i> 删除
                            </el-button>
                        </template>
                    </el-table-column>
                </el-table>
            </el-card>

            <!-- 新增/编辑菜单项 弹出框 -->
            <el-dialog :title="dialogMode === 'add' ? '新增菜单项' : '修改菜单项'" :visible.sync="formVisible" width="500px"
                :close-on-click-modal="false">
                <el-form :model="dialogForm" :rules="formRules" ref="dialogForm" label-width="100px">
                    <el-form-item label="菜单名称" prop="name">
                        <el-input v-model="dialogForm.name" placeholder="请输入菜单名称"></el-input>
                    </el-form-item>
                    <el-form-item label="菜单路径">
                        <el-input v-model="dialogForm.path" placeholder="请输入菜单路径"></el-input>
                    </el-form-item>
                    <el-form-item label="菜单图标" prop="icon">
                        <el-select v-model="dialogForm.icon" filterable placeholder="请选择图标" style="width: 100%;">
                            <el-option v-for="dict in iconDict" :key="dict.itemKey" :label="dict.itemKey"
                                :value="dict.itemValue">
                                <i :class="dict.itemValue"></i>
                                <span style="margin-left: 10px;">{{ dict.itemKey }}</span>
                            </el-option>
                        </el-select>
                    </el-form-item>
                    <el-form-item label="页面路径">
                        <el-input v-model="dialogForm.pagePath" placeholder="请输入页面路径"></el-input>
                    </el-form-item>
                    <el-form-item label="排序" prop="sortNum">
                        <el-input-number v-model="dialogForm.sortNum" :min="0" controls-position="right" style="width: 100%;"></el-input-number>
                    </el-form-item>
                    <el-form-item label="菜单分配" prop="role">
                        <el-checkbox-group v-model="checkList">
                            <el-checkbox label="超级管理员" key="SUPER_ADMIN"></el-checkbox>
                            <el-checkbox label="管理员" key="ADMIN"></el-checkbox>
                            <el-checkbox label="商户" key="MERCHANT"></el-checkbox>
                            <el-checkbox label="普通用户" key="USER"></el-checkbox>
                        </el-checkbox-group>
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

            <!-- 导出数据 弹出栏 -->
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
    name: 'Menu',
    components: { Pagination, Upload, Hints },
    data() {
        return {
            // 数据列表加载动画
            listLoading: true,
            formLabelWidth: '80px',
            iconDict: {},
            checkList: [],
            // 新增/编辑提交表单对象
            dialogForm: {
                name: undefined,
                description: undefined,
            },
            // 数据总条数
            total: 0,
            // 表格数据数组
            tableData: [],
            // 新增/编辑 弹出框显示/隐藏
            formVisible: false,
            // 当前对话框模式
            dialogMode: 'add', // 'add' or 'edit'
            // 导出数据 弹出框显示/隐藏
            exportVisible: false,
            // 防止多次连续提交表单
            isSubmit: false,
            // 表单校验 trigger: ['blur', 'change'] 为多个事件触发
            formRules: {
                // 验证规则
                name: [
                    { required: true, message: '请填写菜单名称', trigger: 'blur' }
                ],
                sortNum: [
                    { required: true, message: '排序不能为空', trigger: 'blur' },
                ],
                icon: [
                    { required: true, message: '请选择菜单图标', trigger: 'change' }
                ]
            }
        }
    },
    created() {
        this.fetchData()
        this.loadIconList()
    },
    methods: {
        handleDelete(index, row) {
            console.log(index, row)
            this.$confirm('此操作将删除选中数据, 是否继续?', '提示', {
                confirmButtonText: '确定',
                cancelButtonText: '取消',
                type: 'warning'
            }).then(() => {
                Request.delete("/menu/deleteById/" + row.id).then(response => {
                    if (response.code == 0) {
                        this.$message({
                            type: 'success',
                            message: '删除成功!'
                        })
                        this.fetchData()
                    } else {
                        this.$message({
                            type: 'error',
                            errpr: '删除失败!'
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
        addChildMenu(pid) {
            this.dialogMode = 'add';
            this.formVisible = true;
            this.dialogForm = {
                pid: pid,
                name: '',
                path: '',
                icon: '',
                pagePath: '',
                description: '',
                sortNum: 0
            };
            this.checkList = [];
            this.loadIconList();
        },
        loadIconList() {
            Request.get("/dictitem/findByType", {
                params: {
                    code: "icon"
                }
            }).then(response => {
                if (response.code === '0') {
                    this.iconDict = response.data;
                    console.log(this.iconDict)
                } else {
                    this.$message({
                        type: 'error',
                        message: '图标列表失败!'
                    })
                }
            });
        },
        // 获取数据列表
        async fetchData() {
            this.listLoading = true
            try {
                const res = await Request.get("/menu/findAll")
                if (res.code === '0') {
                    this.tableData = res.data || []
                    this.total = this.tableData.length
                } else {
                    this.$message.error(res.msg || '获取菜单列表失败')
                }
            } catch (error) {
                console.error('获取菜单列表失败:', error)
                this.$message.error('获取菜单列表失败')
            } finally {
                this.listLoading = false
            }
        },

        openDialog(mode, row = {}) {
            this.dialogMode = mode;
            this.formVisible = true;
            this.loadIconList();
            if (mode === 'edit') {
                this.dialogForm = JSON.parse(JSON.stringify(row));
                this.checkList = this.formatRolesToChinese(row.role);
            } else {
                this.dialogForm = {};
                this.checkList = [];
            }
        },
        // 保存新增/编辑数据
        handleSave(formName) {
            this.dialogForm.role = this.formatRolesToEnglish(this.checkList);
            this.$refs[formName].validate(valid => {
                if (valid) {
                    const saveData = {
                        ...this.dialogForm,
                        pid: this.dialogForm.pid || null
                    };

                    const request = this.dialogMode === 'add' 
                        ? Request.post("/menu/save", saveData) 
                        : Request.put("/menu/" + this.dialogForm.id, saveData);

                    request.then(response => {
                        if (response.code === '0') {
                            this.$message({
                                showClose: true,
                                message: this.dialogMode === 'add' ? '添加成功' : '更新成功',
                                type: 'success',
                            });
                            this.$emit('update:user', this.userInfo);
                            this.formVisible = false;
                            this.fetchData();
                        } else {
                            this.$message({
                                showClose: true,
                                message: response.msg || (this.dialogMode === 'add' ? '添加失败' : '更新失败'),
                                type: 'error',
                            });
                        }
                    }).catch(error => {
                        console.error('保存失败:', error);
                        this.$message.error('操作失败，请重试');
                    }).finally(() => {
                        this.isSubmit = false;
                    });
                } else {
                    this.isSubmit = false;
                }
            });
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
        },
        getMenuRole(role) {
            const roleMap = {
                USER: '普通用户',
                MERCHANT: '商户',
                ADMIN: '管理员',
                SUPER_ADMIN: '超级管理员'
            };

            // 如果role为空，则返回空字符串
            if (!role) {
                return '';
            }

            // 将角色字符串按照逗号分割成数组
            const roles = role.split(',');

            // 使用 map 方法将数组中的每个英文角色转换为中文角色
            const chineseRoles = roles.map((singleRole) => roleMap[singleRole]);

            // 使用 join 方法将转换后的中文角色数组连接成一个逗号分隔的字符串
            return chineseRoles.join(',');
        },
        formatRolesToChinese(role) {

            const roleMap = {
                USER: '普通用户',
                MERCHANT: '商户',
                ADMIN: '管理员',
                SUPER_ADMIN: '超级管理员'
            };

            // 如果role为空，则返回空数组
            if (!role) {
                return [];
            }

            // 将角色字符串按照逗号分割成数组，并去除单引号
            const roles = role.split(',').map(singleRole => singleRole.trim().replace(/'/g, ''));

            // 使用 map 方法将数组中的每个英文角色转换为中文角色
            const chineseRoles = roles.map(singleRole => roleMap[singleRole]);

            // 过滤掉未找到映射的项（如果英文角色不在roleMap中）
            const validChineseRoles = chineseRoles.filter(role => role !== undefined);

            // 返回中文角色数组
            return validChineseRoles;
        },


        formatRolesToEnglish(chineseRoles) {
            const roleMap = {
                普通用户: 'USER',
                商户: 'MERCHANT',
                管理员: 'ADMIN',
                超级管理员: 'SUPER_ADMIN'
            };

            // 如果输入数组为空，则返回空字符串
            if (!chineseRoles || chineseRoles.length === 0) {
                return '';
            }

            // 使用 map 方法将数组中的每个中文角色转换为英文角色
            const englishRoles = chineseRoles.map(singleRole => roleMap[singleRole.trim()]);

            // 使用 join 方法将转换后的英文角色数组连接成一个逗号分隔的字符串
            return englishRoles.join(',');
        }
    }
}
</script>

<style lang="less" scoped>
.menu-manager {
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

.table-card {
    border-radius: 8px;
    box-shadow: 0 1px 4px rgba(0, 0, 0, 0.05);
}

.control-btns {
    display: flex;
    justify-content: flex-end;
    align-items: center;

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

.el-tag {
    margin-right: 5px;
    
    &:last-child {
        margin-right: 0;
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