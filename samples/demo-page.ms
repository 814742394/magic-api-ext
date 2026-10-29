import log;

var sql = """
    select * from test_data
    where name like concat('%', #{name}, '%')
""";

// 分页参数自动从请求读取
var result = db.page(sql, {name: name});

// 事务示例
db.transaction(() => {
    db.update('update test_data set name = #{name} where id = #{id}');
    return true;
});

// 手动事务
var tx = db.transaction();
try {
    db.insert('insert into test_data(id, name) values (#{id}, #{name})');
    tx.commit();
} catch (e) {
    tx.rollback();
    throw e;
}

return result;
