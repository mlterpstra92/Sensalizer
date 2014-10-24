$(document ).ready(function() {
    $("#graphModForm").hide();
    var ratio = $("#canvasContainer").height() / $("#canvasContainer").width();
    //Get the context of the canvas element we want to select
    var c = $('#feedGraph');
    var ct = c.get(0).getContext('2d');
    var ctx = document.getElementById("feedGraph").getContext("2d");
    /*************************************************************************/
    //Run function when window resizes
    $(window).resize(respondCanvas);
    function respondCanvas() {
        c.attr('width', $("#canvasContainer").width());
        c.attr('height', $("#canvasContainer").width() * (ratio));
        //Call a function to redraw other content (texts, images etc)
        //chart = new Chart(ct).Line(data, options);
    }

    //Initial call
    respondCanvas();

    function padLeft(nr, n, str) {
        return Array(n - String(nr).length + 1).join(str || '0') + nr;
    }

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
    var getSortedIndex = function (arr) {
        var index = [];
        for (var i = 0; i < arr.length; i++) {
            index.push(i);
        }
        index = index.sort((function(arr){
            /* this will sort ints in descending order, change it based on your needs */
            return function (a, b) {return ((arr[a] > arr[b]) ? 1 : ((arr[a] < arr[b]) ? -1 : 0));
            };
        })(arr));
        return index;
    };
    var sortMultipleArrays = function (sort, followers) {
        var index = getSortedIndex(sort)
            , followed = [];
        followers.unshift(sort);
        followers.forEach(function(arr){
            var _arr = [];
            for(var i = 0; i < arr.length; i++)
                _arr[i] = arr[index[i]];
            followed.push(_arr);
        });
        var result =  {sorted: followed[0]};
        followed.shift();
        result.followed = followed;
        return result;
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

    var numDatapoints = 10;
    var cycle = 0;
    //Apparently, we eat click events, so use event delegation
    $(this).on('click', 'button', function (e) {

        if (e.currentTarget.id == "applyNumLim"){
            //console.log("Druk op de knop... Comm' on man doo itttt")

            var n = document.getElementById("numDataPoints").value;

            if (cycle > n){
                cycle = n
            }
            console.log(n)
            numDatapoints = n;
        }

        e.preventDefault();
        if (e.currentTarget.id == "addfeed"){

        }
        if (e.currentTarget.id == "selectFeed") {
            var guid = generateGUID();
            var feedID = $(this).parent().parent().find('td')[0].innerHTML.trim();
            var apiKey = $(this).parent().parent().find('td')[3].innerHTML.trim();
            console.log(apiKey);
            console.log(guid);
            $.ajax({
                type: "POST",
                url: "triggerFeed",
                data: {feedid: feedID, apikey: apiKey, guid: guid},
                success: function(a){
                    console.log("cool");
                }
            });

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
                    +'</li>'
                    +'<% } %>'
                    +'</ul>'
            }

            //var chart = new Chart(ct).Line(d, options);
            var legendHTML = document.getElementById("legend");
            if (legendHTML)
                legendHTML.innerHTML = chart.generateLegend();

            document.styleSheets[0].addRule('#legend','list-style: none', 'padding:0','margin:0');


            var RabbitMQIP = "54.171.159.157";
            var ws = new SockJS('http://' + RabbitMQIP + ':15674/stomp');
            var client = Stomp.over(ws);
            // SockJS does not support heart-beat: disable heart-beats
            client.heartbeat.incoming = 0;
            client.heartbeat.outgoing = 0;

            var first = true;
            var chart = null;

            var on_connect = function(x) {
                //id = client.subscribe("/queue/"+guid, function(m){
                id = client.subscribe("/topic/"+guid, function(m){
                    // reply by sending the reversed text to the temp queue defined in the "reply-to" header
                    // console.log("SUCCESS!");
                    var data = JSON.parse(m.body);
                    //client.ack(m);
                    if (first) {
                        //console.log(data);
                        if (data && data.datasets) {
                            var followers = [];
                            for (var j in data.datasets) {
                                for (var q in data.datasets[j])
                                    followers.push(data.datasets[j][q].data)
                            }
                            //console.log(followers)

                            var res = sortMultipleArrays(data.labels, followers);
                            console.log(res);
                            data.labels = res.sorted;
                            var order = data.labels;
                            for (var z = 0; z < res.followed.length; ++z) {
                                data.datasets[0][z] = res.followed[z];
                            }

                        }


                        if (data && data.labels) {
                            for (var i in data.labels)
                                data.labels[i] = new Date(data.labels[i]).toTimeString().split(' ')[0];
                        }
                        console.log(data.labels)
                        data.datasets = data.datasets[0];
                        //add some colors, maximum of four. Add more if more datasets

                        //var colors = ["rgba(200,0,0,1)","rgba(0,200,0,1)","rgba(0,0,200,1)","rgba(200,200,200,1)","rgba(150,0,100,1)","rgba(100,150,0,1)","rgba(0,1000,150,1)","rgba(100,0,150,1)","rgba(200,50,250,1)","rgba(250,100,0,1)","rgba(250,150,50,1)","rgba(250,50,100,1)","rgba(200,50,50,1)","rgba(50,200,50,1)","rgba(50,150,200,1)","rgba(100,200,0,1)","rgba(200,150,150,1)","rgba(50,200,50,1)","rgba(150,150,200,1)","rgba(0,50,250,1)"]

                        for (var z = 0; z < data.datasets.length; ++z) {
                            var color='#'+(Math.random()*0xFFFFFF<<0).toString(16);
                            data.datasets[z].pointColor = color;
                            data.datasets[z].strokeColor = color;
                            data.datasets[z].fillColor = "rgba(255.0, 255.0, 255.0, 1.0)";

                            document.styleSheets[0].addRule('#li'+z, 'display: inline');
                            document.styleSheets[0].addRule('#li'+z+':before','content: "▪"; ' +
                                'color: '+color+';'+
                                'display: inline;' +
                                'vertical-align: middle;' +
                                'position: relative;' +
                                'font-size: 3em;' +
                                'padding-right: 10px');
                        }
                        //console.log(data);

                        chart = new Chart(ct).Line(data, options);
                        $("#graphModForm").show();
                        first = false
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

                    if (cycle > numDatapoints){
                        chart.removeData()
                    }
                    cycle = cycle + 1;
                    chart.update();


                });
            };
            var on_error =  function() {
                console.log('error');
            };
            client.connect('guest', 'guest', on_connect, on_error, '/');
           // setInterval(function(){
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
           // }, 2000);
        }
    });
});