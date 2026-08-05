(function () {
    'use strict';

    var CFG = null;
    var observed = false;
    var lastText = '';
    var qrWatching = false;
    var lastQrSignature = '';

    function pick(selList, root) {
        var r = root || document;
        for (var i = 0; i < selList.length; i++) {
            var els = r.querySelectorAll(selList[i]);
            if (els && els.length) return els[0];
        }
        return null;
    }

    function cfg(selectorKey) {
        return CFG && CFG.selectors && CFG.selectors[selectorKey] ? CFG.selectors[selectorKey] : null;
    }

    function agentName(key) {
        if (CFG && CFG.agents && CFG.agents[key]) return CFG.agents[key];
        return '';
    }

    function visible(el) {
        if (!el) return false;
        var rect = el.getBoundingClientRect();
        return rect.width > 0 && rect.height > 0;
    }

    function findInput() {
        var s = cfg('chatInput');
        if (s) {
            var el = pick(s);
            if (el && visible(el)) return el;
        }
        var all = document.querySelectorAll('textarea, [contenteditable="true"]');
        var best = null;
        for (var i = 0; i < all.length; i++) {
            if (!visible(all[i])) continue;
            var rect = all[i].getBoundingClientRect();
            var area = rect.width * rect.height;
            if (area > 800 && (!best || area > best.area)) {
                best = { el: all[i], area: area };
            }
        }
        return best ? best.el : null;
    }

    function findSendButton(input) {
        var s = cfg('sendButton');
        if (s) {
            var el = pick(s);
            if (el && visible(el)) return el;
        }
        if (input) {
            var node = input.parentElement;
            for (var depth = 0; node && depth < 5; depth++) {
                var btns = node.querySelectorAll('button');
                for (var i = 0; i < btns.length; i++) {
                    if (visible(btns[i])) return btns[i];
                }
                node = node.parentElement;
            }
        }
        var allBtns = document.querySelectorAll('button');
        for (var j = allBtns.length - 1; j >= 0; j--) {
            if (visible(allBtns[j])) return allBtns[j];
        }
        return null;
    }

    function findMessageContainer() {
        var s = cfg('messageList');
        if (s) {
            var el = pick(s);
            if (el) return el;
        }
        var candidates = document.querySelectorAll('main, section, div');
        var best = null;
        for (var i = 0; i < candidates.length; i++) {
            var el = candidates[i];
            var rect = el.getBoundingClientRect();
            if (rect.width < 200 || rect.height < 100) continue;
            var len = (el.innerText || '').length;
            if (len < 30) continue;
            if (!best || len > best.len) best = { el: el, len: len };
        }
        return best ? best.el : null;
    }

    function setReactInput(el, value) {
        if (el.tagName === 'TEXTAREA' || el.tagName === 'INPUT') {
            var proto = el.tagName === 'TEXTAREA' ? window.HTMLTextAreaElement.prototype : window.HTMLInputElement.prototype;
            var setter = Object.getOwnPropertyDescriptor(proto, 'value').set;
            setter.call(el, value);
            el.dispatchEvent(new Event('input', { bubbles: true }));
            el.dispatchEvent(new Event('change', { bubbles: true }));
        } else if (el.isContentEditable) {
            el.focus();
            document.execCommand('selectAll', false, null);
            document.execCommand('insertText', false, value);
        }
    }

    function toDataUrl(src, done) {
        if (!src) { done(null); return; }
        if (src.indexOf('data:') === 0) { done(src); return; }
        try {
            fetch(src).then(function (r) { return r.blob(); }).then(function (b) {
                var fr = new FileReader();
                fr.onload = function () { done(fr.result); };
                fr.onerror = function () { done(null); };
                fr.readAsDataURL(b);
            }).catch(function () { done(null); });
        } catch (e) { done(null); }
    }

    function collectImages(container, done) {
        var imgs = container ? container.querySelectorAll('img') : document.querySelectorAll('img');
        var out = [];
        var pending = 0;
        for (var i = 0; i < imgs.length; i++) {
            var src = imgs[i].src || '';
            if (src.indexOf('data:') === 0 || src.indexOf('http') === 0 || src.indexOf('blob:') === 0) {
                pending++;
                (function (s) {
                    toDataUrl(s, function (d) {
                        if (d) out.push(d);
                        pending--;
                        if (pending === 0) done(out);
                    });
                })(src);
            }
        }
        if (pending === 0) done(out);
    }

    function findStopButton() {
        var btns = document.querySelectorAll('button');
        for (var i = 0; i < btns.length; i++) {
            var t = (btns[i].innerText || '').trim();
            var aria = btns[i].getAttribute('aria-label') || '';
            if (t === '停止' || t === '停止生成' || aria.indexOf('停止') > -1) return btns[i];
        }
        return null;
    }

    function emitDiff() {
        var container = findMessageContainer();
        if (!container) return;
        var text = container.innerText || '';
        var newText = '';
        if (text.length > lastText.length && text.indexOf(lastText) === 0) {
            newText = text.substring(lastText.length);
        } else if (text.length > lastText.length) {
            var idx = text.indexOf(lastText);
            if (idx >= 0) {
                newText = text.substring(idx + lastText.length);
            } else {
                // 结构变化导致无法对齐：保守丢弃，防止把欢迎页等无关内容同步到 App
                newText = '';
            }
        }
        if (newText.length > 0) {
            lastText = text;
            collectImages(container, function (images) {
                var payload = JSON.stringify({ text: newText, images: images });
                window.DoubaoNative.onEvent('diff', payload);
            });
        } else {
            lastText = text;
        }
    }

    function extractQr() {
        var s = cfg('qrImage');
        if (s) {
            var el = pick(s);
            if (el) {
                if (el.tagName === 'CANVAS') {
                    try { return el.toDataURL('image/png'); } catch (e) { return null; }
                }
                if (el.tagName === 'IMG') return el.src || null;
            }
        }
        var canvases = document.querySelectorAll('canvas');
        for (var i = 0; i < canvases.length; i++) {
            try {
                var d = canvases[i].toDataURL('image/png');
                if (d && d.length > 200) return d;
            } catch (e) {}
        }
        var imgs = document.querySelectorAll('img');
        for (var j = 0; j < imgs.length; j++) {
            var src = imgs[j].src || '';
            if (src.indexOf('qr') > -1 || src.indexOf('login') > -1 || src.indexOf('code') > -1) return src;
        }
        for (var k = 0; k < imgs.length; k++) {
            var w = imgs[k].naturalWidth || imgs[k].width;
            if (w >= 100) return imgs[k].src || null;
        }
        return null;
    }

    function pushQr() {
        if (!qrWatching) return;
        var src = extractQr();
        if (!src) return;
        toDataUrl(src, function (data) {
            if (!data) return;
            if (data.length === lastQrSignature.length && data === lastQrSignature) return;
            lastQrSignature = data;
            window.DoubaoNative.onEvent('login-qr', data);
        });
    }

    window.DoubaoBridge = {
        setConfig: function (json) {
            try { CFG = JSON.parse(json); } catch (e) { CFG = null; }
        },

        sendMessage: function (text, agentKey) {
            var name = agentKey ? agentName(agentKey) : '';
            if (name) this.switchAgent(name);
            var input = findInput();
            if (!input) return 'no-input';
            setReactInput(input, text);
            var btn = findSendButton(input);
            var result = 'sent';
            if (btn) {
                btn.click();
            } else {
                input.dispatchEvent(new KeyboardEvent('keydown', { key: 'Enter', code: 'Enter', keyCode: 13, bubbles: true }));
                result = 'sent-enter';
            }
            // 等待 React 将用户消息渲染进列表后重置快照，之后的新增内容才是 AI 回复
            var self = this;
            setTimeout(function () { self.resetSnapshot(); }, 600);
            return result;
        },

        resetSnapshot: function () {
            var container = findMessageContainer();
            lastText = (container ? container.innerText : '') || '';
        },

        switchAgent: function (name) {
            if (!name) return false;
            var cands = document.querySelectorAll('[role="button"], button, [data-testid], [class*="agent"], [class*="tool"]');
            for (var i = 0; i < cands.length; i++) {
                var t = (cands[i].innerText || '').trim();
                if (t === name) { cands[i].click(); return true; }
            }
            for (var j = 0; j < cands.length; j++) {
                var t2 = (cands[j].innerText || '').trim();
                if (t2.indexOf(name) === 0 && t2.length <= name.length + 2) { cands[j].click(); return true; }
            }
            return false;
        },

        clickLogin: function () {
            var cands = document.querySelectorAll('button, a, [role="button"], [class*="login"]');
            for (var i = 0; i < cands.length; i++) {
                var t = (cands[i].innerText || '').trim();
                if (t === '登录' || t === '立即登录') { cands[i].click(); return true; }
            }
            return false;
        },

        isLoggedIn: function () {
            var cookies = document.cookie || '';
            var hasSession = cookies.indexOf('session') > -1 || cookies.indexOf('sid') > -1 || cookies.indexOf('token') > -1;
            var avatar = document.querySelector('[class*="avatar"], [data-testid*="avatar"]');
            var loginBtn = document.querySelector('button, [role="button"]');
            var foundLogin = false;
            if (loginBtn) {
                var t = (loginBtn.innerText || '').trim();
                if (t === '登录' || t === '立即登录') foundLogin = true;
            }
            return JSON.stringify({ loggedIn: hasSession || !!avatar, hasCookie: hasSession, hasAvatar: !!avatar, hasLoginButton: foundLogin });
        },

        startObserving: function () {
            if (observed) return;
            observed = true;
            // 初始化文本快照，避免把页面已有内容（欢迎页/推荐卡片）当作增量同步
            var container = findMessageContainer();
            lastText = (container ? container.innerText : '') || '';
            var target = container || document.body;
            var lastChange = Date.now();
            new MutationObserver(function () {
                lastChange = Date.now();
            }).observe(target, { childList: true, subtree: true, characterData: true, attributes: true });

            setInterval(function () {
                var now = Date.now();
                var stopped = findStopButton();
                if (!stopped && now - lastChange > 1200) {
                    emitDiff();
                }
            }, 800);
        },

        startQrWatch: function () {
            if (qrWatching) return;
            qrWatching = true;
            lastQrSignature = '';
            pushQr();
            setInterval(pushQr, 2000);
        },

        stopQrWatch: function () {
            qrWatching = false;
        },

        getPageInfo: function () {
            return JSON.stringify({ url: location.href, title: document.title });
        }
    };
})();
