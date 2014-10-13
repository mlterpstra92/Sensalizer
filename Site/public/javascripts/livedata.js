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
    var steps = 3;
    var max = 100;
    var options = {
        responsive: true,
        animation: false,
        barValueSpacing: 5,
        barDatasetSpacing: 1,
        showTooltips: true,
        label: {format: 'shortTime'}

    };

    var chart;

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



    //Apparently, we eat click events, so use event delegation
    $(this).on('click', 'button', function (e) {
        e.preventDefault();

        if (e.currentTarget.id == "selectFeed") {
            var feedID = $(this).parent().parent().find('td')[0].innerHTML.trim();
            var apiKey = $(this).parent().parent().find('td')[3].innerHTML.trim();

            $.ajax({
                type: "POST",
                url: "triggerFeed",
                data: {feedid: feedID, apikey: apiKey},
                success: function(a){
                    console.log("cool");
                }
            });
            $.ajax({
                type: "GET",
                url: "getAverages/"+feedID,
                success: function(a){
                    console.log(a);
                    document.getElementById("averageStatistics").innerText = a
                }
            });
            var RabbitMQIP = "54.171.108.54";
            var ws = new SockJS('http://' + RabbitMQIP + ':15674/stomp');
            var client = Stomp.over(ws);
            // SockJS does not support heart-beat: disable heart-beats
            client.heartbeat.incoming = 0;
            client.heartbeat.outgoing = 0;

            var first = true;
            var chart = null;

            var on_connect = function(x) {
                id = client.subscribe("/queue/sensalizer", function(m) {
                    // reply by sending the reversed text to the temp queue defined in the "reply-to" header
                    // console.log("SUCCESS!");
                    var data = JSON.parse(m.body);
                    if (first) {
                        console.log(data);
                        if (data && data.datasets) {
                            var followers = [];
                            for (var j in data.datasets) {
                                for (var q in data.datasets[j])
                                    followers.push(data.datasets[j][q].data)
                            }
                            //console.log(followers)

                            var res = sortMultipleArrays(data.labels, followers);
                            //console.log(res);
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
                        data.datasets = data.datasets[0];
                        console.log(data);
                        chart = new Chart(ct).Line(data, options);
                        first = false
                    }
                    else
                    {
                        var timeString = new Date(data.label).toTimeString().split(' ')[0];
                        console.log(data.current_value);
                        data.current_value = data.current_value.reverse();
                        chart.addData(data.current_value, timeString);
                    }
                    chart.update();
                });
            };
            var on_error =  function() {
                console.log('error');
            };
            client.connect('guest', 'guest', on_connect, on_error, '/');
        }
    });
});