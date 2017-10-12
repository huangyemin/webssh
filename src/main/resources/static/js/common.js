requirejs.config({
    baseUrl: 'js/lib',
    paths: {
        app: '../app',
        jquery: ['https://cdn.bootcss.com/jquery/3.2.1/jquery.min', 'jquery.min'],
        sockjs: ['https://cdn.bootcss.com/sockjs-client/1.1.4/sockjs.min', 'sockjs.min']
    },
    shim: {}
});
