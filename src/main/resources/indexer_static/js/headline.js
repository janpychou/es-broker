// requires tableHelper

var oTable = [];

$(document).ready(function() {
//    var categories = ['推荐', '女装','男装','科技','运动','美妆','母婴','设计','趣玩','食品','家居'];
//    var scenes = ["礼物", "家庭", "恋爱", "海淘", "工作", "休息", "假日", "天气"];
//    var styles = ["品质", "个性", "热门", "优惠", "新潮", "专业", "文艺", "奢侈"];
//    var origins = ["淘宝头条", "必买清单", "严选"];
    var styles=['新潮', '个性', '热门', '实惠', '品质', '奢侈', '大牌', '文艺'];
    var scenes=['天气', '礼物', '假日', '天气晴', '休息', '假日情人节', '礼品', '恋爱', '家庭', '海淘'];
    var categories=['科技', '美食', '穿搭', '宠物', '育儿', '严选推荐', '家居', '运动', '送礼', '海淘',
                '首页', '耍帅', '男装', '淘宝头条', '时尚', '旅行', '挑款师推荐', '生活', '设计',
                '潮玩', '有趣', '美妆', '趣玩', '健康', '女装', '丁磊的好货推荐', '化妆', '爱吃'];
    var origins=['识物', '淘宝头条', '必买清单'];

    var tag_category = $("#tag_category");
    $(categories).each(function(idx, item) {
        var checkbox = $('<input id="category'+idx+'" type="checkbox">' +
                                         '<label id="category-label' + idx + '" for="category'+idx+'">' + item + '</label>'+
                                         '<span>　　</span>');
        tag_category.append(checkbox);
    });

    var tag_scene = $("#tag_scene");
    $(scenes).each(function(idx, item) {
        var checkbox = $('<input id="scene' + idx + '" type="checkbox">' +
                                         '<label id="scene-label' + idx + '" for="scene'+idx+'">' + item + '</label>'+
                                         '<span>　　</span>');
        tag_scene.append(checkbox);
    });

    var tag_style = $("#tag_style");
    $(styles).each(function(idx, item) {
        var checkbox = $('<input id="style'+idx+'" type="checkbox">' +
                                         '<label id="style-label' + idx + '" for="style'+idx+'">' + item + '</label>'+
                                         '<span>　　</span>');
        tag_style.append(checkbox);
    });

    var tag_source = $("#tag_source");
    $(origins).each(function(idx, item) {
        var checkbox = $('<input id="origin'+idx+'" type="checkbox">' +
                                         '<label id="origin-label' + idx + '" for="origin'+idx+'">' + item + '</label>'+
                                         '<span>　　</span>');
        tag_source.append(checkbox);
    });

    $('#searchBtn').click(function(e) {
        var searchText = $('#searchText').val();
        var categorieChecked = [];
        $(categories).each(function(idx, item) {
            if($('#category' + idx).prop("checked") == true) {
                categorieChecked.push($('#category-label' + idx).text());
            }
        });

        var sceneChecked = [];
        $(scenes).each(function(idx, item) {
            if($('#scene' + idx).prop("checked") == true) {
                sceneChecked.push($('#scene-label' + idx).text());
            }
        });

        var styleChecked = [];
        $(styles).each(function(idx, item) {
            if($('#style' + idx).prop("checked") == true) {
                styleChecked.push($('#style-label' + idx).text());
            }
        });

        var sourceChecked = [];
        $(origins).each(function(idx, item) {
            if($('#origin' + idx).prop("checked") == true) {
                sourceChecked.push($('#origin-label' + idx).text());
            }
        });

        postData(searchText, categorieChecked, sceneChecked, styleChecked, sourceChecked);
    });

    function postData(text, categories, scenes, styles, origins) {
        var searchData = {
            'text': text,
            'category': categories,
            'scene': scenes,
            'style': styles,
            'origin': origins,
            'offset': 0,
            'size': 30
        };
        $.ajax({
        	type:"POST",
        	url: '/api/index/search',
        	data: JSON.stringify(searchData),
        	contentType: "application/json; charset=utf-8",
        	success:function(resultData) {
        	    console.info(resultData);
        	    buildSearchTable(resultData);
        	},
        	error: function(data){
        	    console.info(data);
//        		alert('failed with status: ' + data.status + ' statusText:' + data.statusText
//        		    + ' reason:' + data.responseText + ' please check log');
        	}
         });
    }

    function buildSearchTable(resultData) {
        var result = {
            "status": resultData.status,
            "spend": resultData.spend,
            "terminated": resultData.terminated,
            "timeout": resultData.timeout,
            "totalHits": resultData.totalHits,
            "maxScore": resultData.maxScore,
            "totalShards": resultData.totalShards,
            "successfulShards": resultData.successfulShards,
            "failedShards": resultData.failedShards,
            "failure": resultData.failure
        };
        $('#searchResult').text(JSON.stringify(result));
        var items = [];
        $.each(resultData.sources, function(idx, attr){
            var item = {};
            items.push(item);
            var docId = attr.id;
            item.idx = idx;
            item._score = attr._score;
            item.id = attr.id;
            for (var field in attr) {
                item[field] = attr[field];
            }
        });
        buildTable(items, $('#resultTable'));
    }

    $('#searchBtn').click();

    $('#indexBtn').click(function(){
        $.ajax({
            type:"GET",
            url: '/api/index/batch',
            success:function(resultData) {
                $('#indexMsg').text(JSON.stringify(resultData));
            },
            error: function(data){
                console.info(data);
            }
         });
    });
});
