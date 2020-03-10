/**
 * @Auther: wangwei
 * @Date: 2019-07-21 18:11
 * @Description:包工系统角色Enum
 */
public enum RoleEnum {
    /**
     * 项目经理
     **/
    XMJL("项目经理","402835344603175a01461c9abbc801ff"),
    /**
     * 工程师
     **/
    GCS("工程师","402835344603175a01461c9abbb801fe"),
    /**
     * 分管人员
     **/
    FGRY("分管人员","402835344603175a01461c9abbd80200"),
    /**
     * 项目管理员
     **/
    XMGLRY("项目管理员","402835344603175a01461c9abbd80201"),
    /**
     * 系统管理员
     **/
    XTGLY("系统管理员","402835344603175a01461c9643ad003e"),
    /**
     * 用户管理
     **/
    YHGL("用户管理","2c94e5c64a98c6d1014ac8572ca37c3d"),
    /**
     * 查询
     **/
    CX("查询","2c94e5c64f8c0845014f8d0a6df8074c");

    /**
     * @Author wangwei
     * @Description //TODO 用户角色
     * @Date 18:19 2019-07-21
     * @Param 
     * @return 
     **/
    private String roleName;
    /**
     * @Author wangwei
     * @Description //TODO 用户角色id
     * @Date 18:19 2019-07-21
     * @Param
     * @return
     **/
    private String roleVale;

    private RoleEnum(String roleName, String roleVale) {
        this.roleName = roleName;
        this.roleVale = roleVale;
    }

    public String getRoleName(){
        return roleName;
    }

    public String getRoleVale() {
        return roleVale;
    }}






