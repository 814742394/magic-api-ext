// 复现真实工程写法：点号路径 import + lambda + 统一响应结构
import response;
import log;
import db;
import com.alibaba.fastjson.JSON;
import java.util.UUID;
import java.util.HashSet;
import java.util.ArrayList;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;

var ok = (data) => {
    return { status: 1, errorcode: 0, error: null, data: data };
};

// 400 报文结构错误、422 字段非法、500 脚本异常
var fail = (errorcode, error) => {
    return { status: 0, errorcode: errorcode, error: error, data: {} };
};

var body = {
    shipNo: body.shipNo,
    hash: MessageDigest.getInstance('MD5')
};

if (StringUtils.isBlank(body.shipNo)) {
    return fail(422, 'shipNo 不能为空');
}

var rows = db.select("""
    select * from mes_qms_inspect_result
    where ship_no = #{shipNo}
""", {shipNo: body.shipNo});

log.info('匹配到 {} 条', rows.size());

return ok({rows: rows, traceId: UUID.randomUUID().toString()});
