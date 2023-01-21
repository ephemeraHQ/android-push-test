var add = function(x) {
    return function(y) { Android.sendData(x+y); return x + y; };
}