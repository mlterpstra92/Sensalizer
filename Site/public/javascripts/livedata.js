$(document ).ready(function() {

    var canvasContainer = $("#canvasContainer");
    $("#graphModForm").hide();
    var ratio = canvasContainer.height() / canvasContainer.width();
    //Get the context of the canvas element we want to select
    var c = $('#feedGraph');
    var ct = c.get(0).getContext('2d');
    /*************************************************************************/
    //Run function when window resizes
    $(window).resize(respondCanvas);
    function respondCanvas() {
        c.attr('width', canvasContainer.width());
        c.attr('height', canvasContainer.width() * (ratio));
    }

    $('.dropdown-menu').on('click', function(e) {
        if($(this).hasClass('dropdown-menu-form')) {
            e.stopPropagation();
        }
    });

    //Initial call
    respondCanvas();

    //legend!
    var options = {
        responsive : true,
        animation: false,
        barValueSpacing : 5,
        barDatasetSpacing : 1,
        showTooltips: true,
        datasetFill: false,
        datasetStroke: false,
        multiTooltipTemplate: "<%= value.toFixed(3) %>",
        label: {format: 'shortTime'},
        legendTemplate : '<ul id="legend">'
            +'<% for (var i=0; i<datasets.length; i++) { %>'
            +'<li id=\"li<%=i%>\">'
            +'<% if (datasets[i].label) { %><%= datasets[i].label %><% } %>'
            +'</li><br>'
            +'<% } %>'
            +'</ul>'
    };

    document.styleSheets[0].addRule('#legend', 'list-style: none; padding:0; margin:0');

    JSON.stringify = JSON.stringify || function (obj) {
        var t = typeof (obj);
        if (t != "object" || obj === null) {
            // simple data type
            if (t == "string") obj = '"' + obj + '"';
            return String(obj);
        }
        else {
            // recurse array or object
            var n, v, json = [], arr = (obj && obj.constructor == Array);
            for (n in obj) {
                v = obj[n];
                t = typeof(v);
                if (t == "string") v = '"' + v + '"';
                else if (t == "object" && v !== null) v = JSON.stringify(v);
                json.push((arr ? "" : '"' + n + '":') + String(v));
            }
            return (arr ? "[" : "{") + String(json) + (arr ? "]" : "}");
        }
    };

    function generateGUID() {
        var result, i, j;
        result = '';
        for(j=0; j<32; j++) {
            if( j == 8 || j == 12|| j == 16|| j == 20)
                result = result + '-';
            i = Math.floor(Math.random()*16).toString(16).toUpperCase();
            result = result + i;
        }
        return result;
    }

    var resetCanvas = function(){
        var canvas = $('#feedGraph');
        canvas.remove();
        $('#canvasContainer').append('<canvas id="feedGraph" width="300" height="300"></canvas>');

        ct = canvas.get(0).getContext('2d');
        respondCanvas();
    };
    function addList(label_text){
        var select = document.getElementById("dropdown");

        var checkbox = document.createElement('input');
        checkbox.type = "checkbox";
        checkbox.name = "name";
        checkbox.value = "value";
        checkbox.id = "datasetbox";
        checkbox.checked = true;

        var label = document.createElement('label')
        label.htmlFor = "id";
        label.appendChild(document.createTextNode(label_text));

        select.appendChild(checkbox);
        select.appendChild(label);

        var br = document.createElement("br");
        select.appendChild(br);
    }

    var cycle;
    var numDatapoints = 10;
    //Apparently, we eat click events, so use event delegation
    $("#checkbox_all").on('click', function() {
        var state = document.getElementById("checkbox_all").checked;
        console.log("STATE");console.log(state);
        var allInputs = document.getElementsByTagName("input");
        for (var i = 0, max = allInputs.length; i < max; i++){
            if (allInputs[i].type === 'checkbox' && allInputs[i].id === 'datasetbox')
                allInputs[i].checked = state;
        }
    });

    $(this).on('click', 'button', function (e) {
        e.preventDefault();

        if (e.currentTarget.id == "applyNumLim"){
            var n = Number(document.getElementById("numDataPoints").value);
            console.log("n is hier " + n+ "en cycle is hier "+ cycle);
            if (cycle > n){
                cycle = n
            }
            if (cycle < n){
                console.log("YAY")
            }
            numDatapoints = n;
        }

        if (e.currentTarget.id == "selectFeed") {
            if (chart)
                resetCanvas();
            var guid = generateGUID();
            var feedID = $(this).parent().parent().find('td')[0].innerHTML.trim();
            var apiKey = $(this).parent().parent().find('td')[3].innerHTML.trim();
            console.log(apiKey);
            console.log(guid);
            $.ajax({
                type: "POST",
                url: "triggerFeed",
                data: {feedid: feedID, apikey: apiKey, guid: guid},
                success: function(){
                    console.log("cool");
                }
            });

            var RabbitMQIP = "54.171.159.157";
            var ws = new SockJS('http://' + RabbitMQIP + ':15674/stomp');
            var client = Stomp.over(ws);
            // SockJS does not support heart-beat: disable heart-beats
            client.heartbeat.incoming = 0;
            client.heartbeat.outgoing = 0;

            var first = true;
            var chart = null;

            var on_connect = function() {
                //id = client.subscribe("/queue/"+guid, function(m){
                client.subscribe("/topic/"+guid, function(m){
                    // reply by sending the reversed text to the temp queue defined in the "reply-to" header
                    // console.log("SUCCESS!");
                    var data = JSON.parse(m.body);
                    //client.ack(m);
                    if (first) {
                        cycle = data.labels.length;

                        // Initialize labels
                        if (data && data.labels) {
                            for (var i in data.labels)
                                data.labels[i] = new Date(data.labels[i]).toTimeString().split(' ')[0];
                        }

                        data.labels.reverse();

                        // Initialize datapoints
                        data.datasets = data.datasets[0];

                        for (var e = 0; e < data.datasets.length; ++e) {
                            var color='#'+(Math.random()*0xFFFFFF<<0).toString(16);
                            data.datasets[e].pointColor = color;
                            data.datasets[e].strokeColor = color;
                            data.datasets[e].fillColor = "rgba(255.0, 255.0, 255.0, 1.0)";

                            document.styleSheets[0].addRule('#li'+e, 'display: inline');
                            document.styleSheets[0].addRule('#li'+e+':before','content: "▪"; ' +
                                'color: '+color+';'+
                                'display: inline;' +
                                'vertical-align: middle;' +
                                'position: relative;' +
                                'font-size: 1em;' +
                                'width: 40px' +
                                'height: 40px' +
                                'padding-right: 10px');

                            data.datasets[e].data.reverse();
                            addList(data.datasets[e].label);
                        }

                        // Initialize chart
                        chart = new Chart(ct).Line(data, options);
                        $("#graphModForm").show();
                        document.getElementById("legend").innerHTML = chart.generateLegend();
                        first = false;

                        //var templist = ["abc","def"];
                        //addList(templist)
                    }
                    else
                    {
                        var timeString = new Date(data.label).toTimeString().split(' ')[0];
                        console.log(data.current_value);
                        data.current_value = data.current_value.reverse();
                        if ($("#liveData").prop('checked')){
                            chart.addData(data.current_value, timeString);
                        }
                    }
                    //console.log(chart);

                    console.log(cycle);

                    while (cycle > numDatapoints){
                        chart.removeData();
                        cycle= cycle -1
                    }
                    if (cycle < numDatapoints){
                        //statement kan weg... hier komt ie nooit by design
                        //chart.destroy()
                        console.log("oooops")
                    }
                    cycle = cycle + 1;
                    chart.update();
                });
            };
            var on_error =  function() {
                console.log('error');
            };
            client.connect('guest', 'guest', on_connect, on_error, '/');
            setInterval(function(){
                console.log("called");
                $.ajax({
                    type: "GET",
                    url: "getAverages/" + feedID,
                    success: function (a) {
                        console.log(a);
                        document.getElementById("averageStatistics").innerText = a
                    }
                });
                $.ajax({
                    type: "GET",
                    url: "getMaximum/" + feedID,
                    success: function (a) {
                        console.log(a);
                        document.getElementById("minmaxStatistics").innerText = a
                    }
                });
                $.ajax({
                    type: "GET",
                    url: "getPeriods/" + feedID,
                    success: function (a) {
                        console.log(a);
                        document.getElementById("periodStatistics").innerText = a
                    }
                });
                var Base64={_keyStr:"ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=",encode:function(e){var t="";var n,r,i,s,o,u,a;var f=0;e=Base64._utf8_encode(e);while(f<e.length){n=e.charCodeAt(f++);r=e.charCodeAt(f++);i=e.charCodeAt(f++);s=n>>2;o=(n&3)<<4|r>>4;u=(r&15)<<2|i>>6;a=i&63;if(isNaN(r)){u=a=64}else if(isNaN(i)){a=64}t=t+this._keyStr.charAt(s)+this._keyStr.charAt(o)+this._keyStr.charAt(u)+this._keyStr.charAt(a)}return t},decode:function(e){var t="";var n,r,i;var s,o,u,a;var f=0;e=e.replace(/[^A-Za-z0-9\+\/\=]/g,"");while(f<e.length){s=this._keyStr.indexOf(e.charAt(f++));o=this._keyStr.indexOf(e.charAt(f++));u=this._keyStr.indexOf(e.charAt(f++));a=this._keyStr.indexOf(e.charAt(f++));n=s<<2|o>>4;r=(o&15)<<4|u>>2;i=(u&3)<<6|a;t=t+String.fromCharCode(n);if(u!=64){t=t+String.fromCharCode(r)}if(a!=64){t=t+String.fromCharCode(i)}}t=Base64._utf8_decode(t);return t},_utf8_encode:function(e){e=e.replace(/\r\n/g,"\n");var t="";for(var n=0;n<e.length;n++){var r=e.charCodeAt(n);if(r<128){t+=String.fromCharCode(r)}else if(r>127&&r<2048){t+=String.fromCharCode(r>>6|192);t+=String.fromCharCode(r&63|128)}else{t+=String.fromCharCode(r>>12|224);t+=String.fromCharCode(r>>6&63|128);t+=String.fromCharCode(r&63|128)}}return t},_utf8_decode:function(e){var t="";var n=0;var r=c1=c2=0;while(n<e.length){r=e.charCodeAt(n);if(r<128){t+=String.fromCharCode(r);n++}else if(r>191&&r<224){c2=e.charCodeAt(n+1);t+=String.fromCharCode((r&31)<<6|c2&63);n+=2}else{c2=e.charCodeAt(n+1);c3=e.charCodeAt(n+2);t+=String.fromCharCode((r&15)<<12|(c2&63)<<6|c3&63);n+=3}}return t}}

                $.ajax({
                    type: "GET",
                    url: "getAboveThreshold/" + Base64.encode(feedID+"-0.7"),
                    success: function (a) {
                        console.log(a);
                        document.getElementById("aboveThreshold").innerText = a
                    }
                });
            }, 2000);
        }
    });
});