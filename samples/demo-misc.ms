// 语法覆盖：字面量、lambda、内嵌语言、模板串、Linq
var ints = [1, 2, 3];
var i = 1l;
var f = 1.5d;
var m = 12.30m;
var re = /\d+/gi;

var doubled = ints.map(it => it * 2);

var greet = `hello ${name}`;

var calc = ```javascript
    name + ' ~ world'
```;

for (item in ints) {
    log.info('{}', item);
}

for (k, v in map) {
    log.info('{} = {}', k, v);
}

var picked = select id, name from user where id > 1 order by id desc limit 10;

return {doubled: doubled, greet: greet()};
