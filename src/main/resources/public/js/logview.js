window.Logview = (function app(window, document) {
    'use strict';

    /**
     * @type {Object}
     * @private
     */
    var _socket;

    /**
     * @type {HTMLElement}
     * @private
     */
    var _logContainer;

    /**
     * @type {number}
     * @private
     */
    var _linesLimit = Math.Infinity;

    /**
     * @type {object}
     * @private
     */
    var _highlightConfig;

    /**
     * @return {Boolean}
     * @private
     */
    var _isScrolledBottom = function() {
        var currentScroll = document.documentElement.scrollTop || document.body.scrollTop;
        var totalHeight = document.body.offsetHeight;
        var clientHeight = document.documentElement.clientHeight;
        return totalHeight <= currentScroll + clientHeight;
    };

    /**
     * @return String
     * @private
     */
    var _highlightWord = function(line) {
        if (_highlightConfig) {
            if (_highlightConfig.words) {
                for (var wordCheck in _highlightConfig.words) {
                    if (_highlightConfig.words.hasOwnProperty(wordCheck)) {
                        line = line.replace(
                            wordCheck,
                            '<span style="' + _highlightConfig.words[wordCheck] + '">' + wordCheck + '</span>'
                        );
                    }
                }
            }
        }

        return line;
    };

    /**
     * @return HTMLElement
     * @private
     */
    var _highlightLine = function(line, container) {
        if (_highlightConfig) {
            if (_highlightConfig.lines) {
                for (var lineCheck in _highlightConfig.lines) {
                    if (line.indexOf(lineCheck) !== -1) {
                        container.setAttribute('style', _highlightConfig.lines[lineCheck]);
                    }
                }
            }
        }

        return container;
    };

    return {
        /**
         * Init websocket communication and log container
         *
         * @param {Object} opts options
         */
        init: function init(opts) {
            var self = this;

            // Elements
            _logContainer = opts.container;

            // WebSocket
            _socket = opts.socket;
            _socket.onmessage = function(evt) {
                try {
                    var json = JSON.parse(evt.data);
                    _linesLimit = json.lines;
                    _highlightConfig = json.highlight;
                    if (json.noindent === 'true') {
                        _logContainer.className += ' no-indent';
                    }
                } catch (e) {
                    self.log(evt.data);
                }
            };
        },

        /**
         * Log data
         *
         * @param {string} data data to log
         */
        log: function log(data) {
            var wasScrolledBottom = _isScrolledBottom();
            var div = document.createElement('div');
            var p = document.createElement('p');
            p.className = 'inner-line';

            p.innerHTML = _highlightWord(data);

            div.className = 'line';
            div = _highlightLine(data, div);

            div.appendChild(p);
            _logContainer.appendChild(div);

            if (_logContainer.children.length > _linesLimit) {
                _logContainer.removeChild(_logContainer.children[0]);
            }

            if (wasScrolledBottom) {
                window.scrollTo(0, document.body.scrollHeight);
            }
        }
    };
}(window, document));