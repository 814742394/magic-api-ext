// magic-api 示例：用户接口
// 路径 /user/{id}，方法 GET
import log;
import request;
import response;

var id = path.id;

if (id == null) {
    exit 400, 'id 不能为空';
}

var user = db.selectOne("""
    select id, name, created_at
    from user
    where id = #{id}
""");

if (user == null) {
    exit 404, '用户不存在', {id: id};
}

log.info('查询用户 {}', id);

// Linq 风格筛选
var active = select id, name from user where id > 1 limit 20;

// 集合扩展方法
var names = active.map(it => it.name).join(',');

return {
    code: 1,
    data: user,
    names: names,
    clientIp: request.getClientIP()
};
