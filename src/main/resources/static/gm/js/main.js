let options = {'storageName': 'cameo', 'menuStateStorage': true};
let sidebar = {
	initialize: function() {
		if ($('body > .app').data('sidebar') !== 'locked') {
			sidebar.menuState();
		}
	}, menuState: function() {
		if (window.localStorage.getItem(options.storageName + '_navigation') !== null && options.menuStateStorage === true) {
			if (window.localStorage.getItem(options.storageName + '_navigation') === '0') {
				sidebar.openMenuState();
			} else {
				sidebar.closeMenuState();
			}
		}
	}, closeMenuState: function() {
		$('body > .app').addClass('small-sidebar');
		$('.toggle-sidebar i').removeClass('fa-angle-left').addClass('fa-angle-right');
	}, openMenuState: function() {
		$('body > .app').removeClass('small-sidebar');
		$('.toggle-sidebar i').removeClass('fa-angle-right').addClass('fa-angle-left');
	}
};
let MODULE_NAME = (location.pathname.split('/'))[1];

if(window.top.document !== window.document){
	$('html').addClass('iframe');
}
window.onresize = function(){
	$('body > .app').removeClass('small-sidebar');
	if ($(window).width() > 768) sidebar.initialize();
};

//关联
$.fn.goodsModal = function(options){
	options = $.extend({
		jsonList: 'list',
		type: 'goods', //[goods|category]
		text: '',
		api: '',
		target: null
	}, options);
	return this.each(function(){
		let _this = $(this), ids = [];
		$(options.target).on('click', 'a.del', function(){
			let parent = $(this).parent();
			parent.sideUp(300, function(){parent.remove()});
		});
		$(options.target).find(':hidden').each(function(){ids.push($(this).val())});
		_this.on('click', function(){
			let win = $(window), modal = _this.data('goodsModal');
			if(!!!modal){
				modal = $('<div class="panel-body goods">\
					<div class="title ge-bottom ge-light"><a href="javascript:void(0)">×</a>搜索'+options.text+'</div>\
					<div class="view">\
						<table>\
							<thead>\
								<tr>\
									'+(options.type === 'goods' ? '<th class="pic">图片</th>' : '')+'\
									<th class="name">\
										<font>名称</font>\
										<div><span>搜索</span><input type="text" class="keyword" placeholder="输入'+options.text+'搜索" /></div>\
									</th>\
									'+(options.type === 'goods' ? '<th class="price">价格</th>' : '')+'\
									<th class="btnView"></th>\
								</tr>\
							</thead>\
						</table>\
						<div class="list">\
							<table><tbody></tbody></table>\
						</div>\
					</div>\
					<div class="bottom ge-top ge-light">\
						<div class="page"></div>\
						<a href="javascript:void(0)" class="selected hidden"></a>\
					</div>\
				</div>');
				_this.data('goodsModal', modal);
			}
			if(win.width <= 768)modal.css({ width:win.width, height:win.height });
			else modal.css({ width:'', height:'' });
			$.overlay(modal, 0);
			modal.find('.title a').on('click', function(){$.overlay(false)});
			modal.find('.keyword').onkey({
				callback: function(code){
					if(code === 13)setList(modal, $(this).val());
				}
			}).prev().on('click', function(){
				setList(modal, modal.find('.keyword').val());
			});
			setList(modal, modal.find('.keyword').val());
		});
		function setList(modal, keyword, page){
			if(!page)page = 1;
			page = Number(page);
			$.getJSON(options.api+(options.api.indexOf('?')>-1?'&':'?')+'page='+page+'&paginate=paginate&keyword='+keyword, function(json){
				if(!$.isArray(json.data[options.jsonList]))return;
				let html = '', items = [];
				if($.isArray(json.data[options.jsonList])){
					$.each(json.data[options.jsonList], function(){
						let name = [];
						if(options.type === 'goods'){
							name.push('<a href="/'+MODULE_NAME+'/'+options.type+'/edit?id='+this.id+'" target="_blank">'+this.name+'</a>');
						}else{
							$.each(this.parents, function(i, _this){
								name.push('<a href="/'+MODULE_NAME+'/'+options.type+'/edit?id='+_this.id+'" target="_blank">'+_this.name+'</a>');
							});
						}
						html += '<tr>\
							'+(options.type === 'goods' ? '<td class="pic"><div url="'+this.pic+'"></div></td>' : '')+'\
							<td class="name">'+name.join('<em>»</em>')+'</td>\
							'+(options.type === 'goods' ? '<td class="price red">￥'+this.price+'</td>' : '')+'\
							<td class="btnView"><a href="javascript:void(0)" '+($.inArray(this.id, ids) > -1 ? 'class="selected"' : '')+'></a></td>\
						</tr>';
						if($.inArray(this.id, ids) > -1)items.push(this);
					});
				}
				modal.find('.list').scrollTop(0);
				modal.find('.list tbody').html(html);
				modal.find('.list tbody .pic div').loadbackground();
				let paginate = json.data.paginate;
				html = '<ul class="pagination"><font>当前第'+paginate.current_page+'页，共'+paginate.last_page+'页</font> ';
				if(paginate.current_page === 1){
					html += '<li class="disabled"><span>&laquo;</span></li> ';
				}else{
					html += '<li><a href="javascript:void(0)" page="'+(paginate.current_page-1)+'">&laquo;</a></li> ';
				}
				if(paginate.current_page === paginate.last_page){
					html += '<li class="disabled"><span>&raquo;</span></li>';
				}else{
					html += '<li><a href="javascript:void(0)" page="'+(paginate.current_page+1)+'">&raquo;</a></li>';
				}
				html += '</ul>';
				modal.find('.bottom .page').html(html);
				modal.find('.bottom .page a').on('click', function(){
					setList(modal, keyword, $(this).attr('page'));
				});
				modal.find('.bottom .selected').on('click', function(){
					let html = '';
					$.each(items, function(){
						if(options.type === 'goods'){
							html += '<li class="item">\
								<a class="del" href="javascript:void(0)">－</a>\
								<a href="/'+MODULE_NAME+'/'+options.type+'/edit?id='+this.id+'" target="_blank">\
									<div style="background-image:url('+this.pic+');"></div>\
									<span>'+this.name+'</span>\
									<font>￥'+this.price+'</font>\
								</a>\
								<input type="hidden" class="goods_id" name="goods[]" value="'+this.id+'" />\
							</li>';
						}else{
							let name = [];
							$.each(this.parents, function(i, _this){
								name.push('<a href="/'+MODULE_NAME+'/'+options.type+'/edit?id='+_this.id+'" target="_blank">'+_this.name+'</a>');
							});
							html += '<li class="row">\
								<a class="del" href="javascript:void(0)">－</a>\
								'+name.join('<em>»</em>')+'\
								<input type="hidden" class="goods_id" name="goods[]" value="'+this.id+'" />\
							</li>';
						}
					});
					$(options.target).html(html);
					$.overlay(false);
				});
				modal.find('.list td.btnView a').each(function(i){
					$(this).data('data', json.data[options.jsonList][i]).on('click', function(){
						let data = $(this).data('data');
						if($(this).hasClass('selected')){
							$(this).removeClass('selected');
							let idsTmp = [], itemsTmp = [];
							$.each(items, function(){
								if(Number(data.id) !== Number(this.id)){
									idsTmp.push(this.id);
									itemsTmp.push(this);
								}
							});
							ids = idsTmp;
							items = itemsTmp;
						}else{
							$(this).addClass('selected');
							ids.push(data.id);
							items.push(data);
						}
						if(items.length)modal.find('.bottom .selected').removeClass('hidden');
						else modal.find('.bottom .selected').addClass('hidden');
					});
				});
			});
		}
	});
};

function configs() {
	if ($.isFunction($.fn.selectpicker)) {
		$('.col-selectpicker').not('[data-configs]').attr('data-configs', 'init').each(function(){
			$(this).selectpicker();
		});
	}
	if ($.isFunction($.fn.datepicker)) {
		$('.col-datepicker').not('[data-configs]').attr('data-configs', 'init').each(function(){
			$(this).datepicker();
		});
	}
	if ($.isFunction($.fn.colorpicker)) {
		$('.col-color').not('[data-configs]').attr('data-configs', 'init').each(function(){
			let options = {
				readonly: false, callback:function(color){
					this.css('background', color).attr('color', color)
				}
			};
			if ($(this).prev('input').length) options.target = $(this).prev();
			$(this).colorpicker(options);
		});
	}
	if ($.browser().mobile) {
		$('.col-unit + [class*="col-sm-"] > input').each(function(){
			$(this).css('padding-left', $(this).parent().prev().outerWidth(false) + 12);
		});
	}
	$(':radio[data-type="radio"], :checkbox[data-type="checkbox"], :checkbox[data-type="checkbox-app"]').not('[data-configs]').attr('data-configs', 'init').each(function(){
		let _this = $(this), type = _this.attr('data-type'), noLeft = _this.attr('data-noLeft'), label = '';
		if (type === 'checkbox-app') {
			label = '<label class="'+type+(noLeft?'':' pull-left')+'"'+(!!_this.attr('data-style')?' style="'+_this.attr('data-style')+'"':'')+'><i></i></label>';
		} else {
			label = '<label class="'+type+' '+type+'-custom'+(noLeft?'':' pull-left')+'"'+(!!_this.attr('data-style')?' style="'+_this.attr('data-style')+'"':'')+'><i></i>'+((!!_this.attr('data-text') && _this.attr('data-text').length)?' '+_this.attr('data-text'):'')+'</label>';
		}
		_this.before(label);
		if(!!_this.attr('data-class'))_this.prev().addClass(_this.attr('data-class'));
		label = _this.prev();
		label.prepend(_this);
		if (type === 'checkbox-app' && !!_this.attr('data-text') && _this.attr('data-text').length) {
			let id = _this.attr('id');
			if (!!!id) {
				id = 'checkbox_' + $.randomCode(8);
				_this.attr('id', id);
			}
			label.after(' <label'+(!!_this.attr('data-text-class')?' class="'+_this.attr('data-text-class')+'"':'')+(!!_this.attr('data-text-style')?' style="'+_this.attr('data-text-style')+'"':'')+' for="'+id+'">'+_this.attr('data-text')+'</label>');
		}
	});
	$('.col-file').not('[data-configs]').attr('data-configs', 'init').each(function(){
		let _this = $(this), name = _this.attr('data-name'), url = _this.attr('data-url'), text = _this.attr('data-text')||'请选择文件',
			items = _this.attr('data-item'), item = items ? items.split('|')[0] : null, before = _this.attr('data-before'), callback = _this.attr('data-callback');
		let html = '<input type="file" '+(url ? '' : 'name="'+name+'" id="'+name+'"')+' value="" />\
			<span>'+(item ? item : text)+'</span>' + (url ? '' : '<input type="hidden" name="origin_'+name+'" id="origin_'+name+'" value="'+(item ? item : '')+'">');
		_this.html(html);
		if (item) {
			let el = _this;
			if (_this.parent().is('[class*="col-sm-"]')) el = _this.parent();
			el.after('<div class="col-control"><a href="'+item+'" target="_blank"><img src="'+item+'" style="height:34px;" onerror="this.src=\'/static/images/nopic.png\'" /></a></div>');
		}
		if (!url) {
			_this.find('input').on('change', function(){
				_this.find('span').addClass('selected').html(this.files[0].name);
			});
			return true;
		}
		if (before) before = eval(before);
		if (callback) callback = eval(callback);
		_this.find('input').html5upload({
			url : url,
			name : name,
			data: {filename:name},
			before : before, //返回false可取消上传
			success : function(json){
				$.overload(false);
				if (!!!_this.attr('data-text') && $.isFunction(callback)) _this.find('span').addClass('selected').html(json.data[0]);
				if ($.isFunction(callback)) callback(json);
			}
		});
	});
	let splitBefore = function(e, count, files){
			let _this = this;
			if(_this.is('[type="file"]'))_this = _this.parent();
			let getSize = function(size){
				//求次幂
				let pow1024 = function(num){return Math.pow(1024, num)};
				if(!size)return '';
				if(size < pow1024(1))return size + 'B';
				if(size < pow1024(2))return (size / pow1024(1)).toFixed(2) + 'KB';
				if(size < pow1024(3))return (size / pow1024(2)).toFixed(2) + 'MB';
				if(size < pow1024(4))return (size / pow1024(3)).toFixed(2) + 'GB';
				return (size / pow1024(4)).toFixed(2) + 'TB';
			};
			_this.addClass('col-splitfile-selected').find('i').html(files[0].name + ' - ' + getSize(files[0].size));
			_this.find('[type="file"]').css('top', '-9999px');
			let fn = _this.attr('data-before');
			if(!!fn){
				let func = eval(fn);
				if($.isFunction(func)){
					let result = func.call(_this, e, count, files);
					if(typeof result === 'boolean' && !result)return false;
				}
			}
		},
		splitProgress = function(e){
			let _this = this, percent = Math.round((e.loaded / e.total) * 100, 1);
			if(_this.is('[type="file"]'))_this = _this.parent();
			_this.find('span').css({width:percent+'%'}).html(percent+'%');
			if(Number(percent) >= 100)_this.find('span').html('转换中，请稍候...');
		},
		splitSuccess = function(json){
			let _this = this;
			if(_this.is('[type="file"]'))_this = _this.parent();
			_this.find('span').css({width:'100%'}).html('上传完成');
			_this.find('input').not('[type="file"]').val(json.data);
			if(_this.next('div').length)_this.next('div').remove();
			_this.after('<div style="clear:both;padding-top:15px;"><video src="'+json.data+'" controls style="width:220px;height:220px;"></video></div>');
			let fn = _this.attr('data-success');
			if(!!fn){
				let func = eval(fn);
				if($.isFunction(func))func.call(_this);
			}
		},
		splitError = function(){
			let _this = this;
			if(_this.is('[type="file"]'))_this = _this.parent();
			_this.find('[type="file"]').css('top', '');
			let fn = _this.attr('data-error');
			if(!!fn){
				let func = eval(fn);
				if($.isFunction(func))func.call(_this);
			}
		};
	let splitfile = $('.col-splitfile').not('[data-configs]').attr('data-configs', 'init').html5upload({
		splitSize: 20*1024*1000,
		before: splitBefore,
		progress: splitProgress,
		success: splitSuccess,
		error: splitError
	});
	splitfile.prepend('<i>点击选择或文件拖放到这里</i><span></span>').append('<input type="file" '+(!!splitfile.attr('data-fileType') ? 'data-fileType="'+splitfile.attr('data-fileType')+'"' : '')+' />');
	splitfile.find('[type="file"]').not('[data-configs]').attr('data-configs', 'init').attr('data-url', splitfile.attr('data-url')).html5upload({
		splitSize: 20*1024*1000,
		before: splitBefore,
		progress: splitProgress,
		success: splitSuccess,
		error: splitError
	});
	main.setUploader('.col-uploader');
}

$(function () {
	FastClick.attach(document.body);
	if ($(window).width() > 768) sidebar.initialize();
	$('.no-touch .slimscroll').each(function() {
		let data = $(this).data();
		$(this).slimScroll(data);
	});
	$('.toggle-sidebar').on('click', function() {
		if ($('body > .app').hasClass('small-sidebar')) {
			sidebar.openMenuState();
			window.localStorage.setItem(options.storageName + '_navigation', '0');
		} else {
			sidebar.closeMenuState();
			window.localStorage.setItem(options.storageName + '_navigation', '1');
		}
	});
	if ($.isFunction($.fn.control)) {
		$('.quickmenu > a').control({
			expr: '.quickmenu > a',
			hide: function() {
				this.parent().removeClass('collapse-open');
			}
		}).on('click', function() {
			let parent = $(this).parent(), menu = $(this).next();
			if (!menu.is('ul')) return;
			if (menu.is(':visible')) {
				parent.removeClass('collapse-open');
			} else {
				parent.addClass('collapse-open');
			}
		});
		$('.quickmenu .dropdown-menu a').on('click', function() {
			$('.quickmenu').removeClass('collapse-open');
		});
	}
	$('.collapsible .main-navigation > ul> li > a').on('click', function() {
		let parent = $(this).parent(), menu = $(this).next();
		if (!menu.is('ul') || ($('body > .app').hasClass('small-sidebar') && $(window).width() > 768)) return;
		if (menu.is(':visible')) {
			menu.slideUp('normal', () => parent.removeClass('collapse-open'));
		} else {
			parent.siblings('.collapse-open').each(function(){
				$(this).children('ul').slideUp('normal', () => $(this).removeClass('collapse-open'));
			});
			menu.slideDown('normal', () => parent.addClass('collapse-open'));
		}
	});
	//============================================================================================================================================
	main.setEditor('.ckeditor');
	$('table[data-save]').on('keyup', '[type="text"], [type="password"], [type="number"]', function(e){
		let code = e.which||e.keyCode;
		if (code === 13) {
			$(this).parent().parent().find('a[data-save-id]').trigger('click');
			return false;
		}
	});
	$('table[data-save]').on('click', 'a[data-save-id]', function(){
		let _table = $(this).parents('table').eq(0), saveUrl = _table.attr('data-save'),
			validates = _table.attr('data-save-validate-field') ? _table.attr('data-save-validate-field').split(',') : [];
		let tr = $(this).parent().parent();
		let items = tr.find('input[name], textarea[name], select[name]');
		if (validates) {
			let checkboxAndRadio = [], errorMsg = '', errorItem = null;
			for (let i = 0; i < validates.length; i++) {
				let item = items.filter('[name="'+validates[i]+'"]');
				if (item.is(':checkbox') || item.is(':radio')) {
					if (item.is(':checkbox') && item.parent().is('.checkbox-app')) {
						if (!item.is(':checked')) {
							errorMsg = '必须选择';
							errorItem = item;
							break;
						}
						continue;
					}
					let name = item.attr('name');
					if ($.inArray(name, checkboxAndRadio) > -1) continue;
					checkboxAndRadio.push(name);
					if (!tr.find('[name="'+name+'"]:checked').length) {
						errorMsg = '最少必须选中一个';
						errorItem = null;
						break;
					}
				} else if (item.is('select') && !item.find('option:selected').val().length) {
					errorMsg = '请选择';
					errorItem = item;
					break;
				} else if (!item.val().length) {
					errorMsg = '请填写';
					errorItem = item;
					break;
				}
			}
			if (errorMsg.length) {
				main.alert(errorMsg, !errorItem ? null : {
					event: errorItem,
					callback: function(){
						errorItem.focus();
					}
				});
				return false;
			}
		}
		let row = {};
		items.each(function(){
			let item = $(this), name = item.attr('name');
			if (item.is(':checkbox') || item.is(':radio')) {
				if (item.is(':checkbox') && item.parent().is('.checkbox-app')) {
					row[name] = item.is(':checked') ? 1 : 0;
					return true;
				}
				if (item.is(':checked')) {
					row[name] = item.val();
				}
			} else if (item.is('select')) {
				row[name] = item.find('option:selected').val();
			} else {
				row[name] = item.val();
			}
		});
		if ($(this).attr('data-save-id')) row['id'] = $(this).attr('data-save-id');
		main.ajaxPost(saveUrl, row);
	});
	$('table[data-save-all]').each(function(){
		let _table = $(this), saveUrl = _table.attr('data-save-all'),
			validates = _table.attr('data-save-validate-field') ? _table.attr('data-save-validate-field').split(',') : [];
		let saveAll = $('<div class="btn-more"><button type="button" class="btn btn-warning">提交全部</button></div>');
		if (_table.parent().is('.panel-table')) {
			_table.parent().after(saveAll);
		} else {
			_table.after(saveAll);
		}
		saveAll.find('button').on('click', function(){
			let param = [];
			_table.find('tbody tr').each(function(){
				let tr = $(this), items = tr.find('input[name], textarea[name], select[name]');
				if (validates) {
					let checkboxAndRadio = [], errorMsg = '';
					for (let i = 0; i < validates.length; i++) {
						let item = items.filter('[name="'+validates[i]+'"]');
						if (item.is(':checkbox') || item.is(':radio')) {
							if (item.is(':checkbox') && item.parent().is('.checkbox-app')) {
								if (!item.is(':checked')) {
									errorMsg = '必须选择';
									break;
								}
								continue;
							}
							let name = item.attr('name');
							if ($.inArray(name, checkboxAndRadio) > -1) continue;
							checkboxAndRadio.push(name);
							if (!tr.find('[name="'+name+'"]:checked').length) {
								errorMsg = '最少必须选中一个';
								break;
							}
						} else if (item.is('select') && !item.find('option:selected').val().length) {
							errorMsg = '请选择';
							break;
						} else if (!item.val().length) {
							errorMsg = '请填写';
							break;
						}
					}
					if (errorMsg.length) return true;
				}
				let row = {};
				items.each(function(){
					let item = $(this), name = item.attr('name');
					if (item.is(':checkbox') || item.is(':radio')) {
						if (item.is(':checkbox') && item.parent().is('.checkbox-app')) {
							row[name] = item.is(':checked') ? 1 : 0;
							return true;
						}
						if (item.is(':checked')) {
							row[name] = item.val();
						}
					} else if (item.is('select')) {
						row[name] = item.find('option:selected').val();
					} else {
						row[name] = item.val();
					}
				});
				if (tr.find('a[data-save-id]').length) row['id'] = tr.find('a[data-save-id]').attr('data-save-id');
				param.push(row);
			});
			main.ajaxPost(saveUrl, {param: param});
		});
	});
	$('.table thead th[data-sortby]').each(function(){
		let matcher = location.href.match(/sortby=((\w+)(?:,|%2C)(\w+))/);
		if (matcher) $(this).addClass(matcher[3]);
		$(this).append('<i></i>').on('click', function(){
			//<th data-sortby="clicks">点击数</th>
			let href = location.href, sort = 'desc', sortby = $(this).attr('data-sortby'), matcher = href.match(/sortby=((\w+)(?:,|%2C)(\w+))/);
			href = href.replace(/&sortby=(\w+(?:,|%2C)\w+)?/, '').replace(/\?sortby=(\w+(?:,|%2C)\w+)?&?/, '?');
			if (matcher) {
				if (matcher[3] === 'desc') {
					href += (/\?/.test(href) ? (/\?$/.test(href) ? '' : '&')+'sortby=' : '?sortby=') + sortby + ',asc';
				}
			} else {
				href += (/\?/.test(href) ? (/\?$/.test(href) ? '' : '&')+'sortby=' : '?sortby=') + sortby + ',' + sort;
			}
			location.href = href;
		});
	});
	let dataTemplate = $('.table tbody tr.template');
	if(dataTemplate.length){
		dataTemplate.find('label.checkbox-app, label.checkbox-custom, label.radio-custom').each(function(){
			let _this = $(this), input = _this.find('input');
			_this.after(input);
			_this.remove();
		});
		let btn = $('<div class="btn-more"><button type="button" class="btn btn-default" style="border-radius:2px;"><i class="fa fa-list-ul"></i>加载更多数据</button></div>'),
			template = dataTemplate.removeClass('template').outerHTML(),
			table = $('.table'), tbody = table.find('tbody'),
			tbodyHtml = tbody.outerHTML(),
			list = dataTemplate.attr('data-list') || 'list',
			listKey = dataTemplate.attr('data-list-key') || 'g',
			mypage = $('.panel-body .page');
		dataTemplate.remove();
		if(mypage.find('.active').length){
			if (table.parent().is('.panel-table')) {
				table.parent().after(btn);
			} else {
				table.after(btn);
			}
		}
		template = template.replace(/data-configs="init"/g, '');
		let tbodyMatcher = tbodyHtml.match(/<tbody([\s\S]+?)>[\r\n]/);
		let re = new RegExp('\\b([\\w\\-]+)="([^"]+)"', 'g'), matcher;
		while((matcher = re.exec(tbodyMatcher[1])) !== null){
			//在tbody获取例如 data-param="abc"，模板内的 [data-param] 字符串会替换为 abc
			template = template.replace(new RegExp('\\['+matcher[1]+'\\]', 'g'), matcher[2]);
		}
		btn.on('click', function(){
			tbody.data('template')();
		});
		tbody.data('template', function(data){
			if(typeof data !== 'undefined')return setData(data);
			let href = !!tbody.data('href') ? tbody.data('href') : location.href;
			if(href.indexOf('?')===-1)href += '?';
			href = href.replace(/\?(page=(\d+)(&?))?/, function(_$, $1, $2, $3){
				if(typeof $1 === 'undefined')return '?page=2';
				return '?page=' + (Number($2)+1) + $3;
			});
			console.log(href)
			tbody.data('href', href);
			$.overload();
			$.getJSON(href, function(json){
				setData(json.data[list]);
			});
			function setData(_data){
				if($.isArray(_data) && _data.length){
					let list = '', errorShown = false;
					$.each(_data, function(){
						let code = 'let $ = window.jQuery, '+listKey+' = this;\n';
						code += $.template(template);
						try{
							try{
								let tbFn = eval('templateBefore');
								if(typeof(tbFn) === 'function'){
									let result = tbFn(code);
									if(typeof(result) === 'string')code = result;
								}
							}catch(e){}
							//console.log(code);
							let html = new Function(code).apply(this);
							html = $.template(html, true);
							try{
								let tcFn = eval('templateComplete');
								if(typeof(tcFn) === 'function'){
									let result = tcFn(html);
									if(typeof(result) === 'string')html = result;
								}
							}catch(e){}
							list += html;
						}catch(e){
							if(!errorShown){
								console.log(template);
								console.log(code);
								console.log(e);
								errorShown = true;
							}
						}
					});
					tbody.append(list);
					configs();
				}else{
					if(typeof data === 'undefined')btn.remove();
				}
				return false;
			}
		});
	}
	if(!$.browser().mobile){
		$(document).on('click', '.iframe-layer', function(e){
			if(e.metaKey || e.ctrlKey)return true;
			let _this = $(this), max = _this.attr('iframe-layer-max'), title = _this.attr('title'), width = _this.attr('width'), height = _this.attr('height'), scrolling = '',
				url = _this.attr('url') || _this.attr('href');
			if(/^#+$/.test(url) || url.indexOf('javascript:')>-1)return true;
			if(!!_this.attr('scrolling'))scrolling = 'scrolling="'+_this.attr('scrolling')+'"';
			main.iframeLayer(url, title, width, height, scrolling, max);
			return false;
		});
		if(window.parent.document !== window.document){
			let classUrl = $.base64().encode(window.location.pathname+window.location.search).replace(/[=/]/g, '');
			let layer = $('.iframe-layer-'+classUrl+', .iframe-layer-'+$.base64().encode('/'+MODULE_NAME+'/login').replace(/[=/]/g, ''), window.parent.document);
			if(layer.length>1)layer = layer.last();
			layer.find('.layer-content .db-circle-ico').remove();
			if(layer.find('.layer-header h6').length && !layer.find('.layer-header h6').html().length && $('header.panel-heading').length){
				let html = $('header.panel-heading').html();
				//html = html.replace(/<div[\s\S]+\/div>/, '');
				layer.find('.layer-header h6').html(html);
				layer.find('.layer-header h6 a').each(function(){
					let _link = $(this);
					if(!!!_link.attr('href') || _link.attr('href').indexOf('javascript:') > -1){
						_link.remove();
						return true;
					}
					_link.attr('target', 'iframe-layer-case-'+classUrl);
				});
			}
		}
	}
	configs();
});

let main = {
	alert: function(message, option) {
		return $.alert({
			title: '<strong style="color:#c7254e;font-size:16px;">温馨提示</strong>',
			content: '<div class="text-center" style="border-top:1px solid #eee;padding-top:20px;">' + message + '</div>',
			confirmButton: '确定',
			confirmButtonClass: 'btn btn-info',
			animation: 'scaleY',
			theme: 'material',
			confirm: function() {
				if (option) {
					if (option.toUrl) {
						window.location.href = option.toUrl
					} else if (option.callback) {
						let event = option.event
						option.callback.call(event, event)
					} else if (!option.stay) {
						window.location.reload()
					}
				}
			}
		});
	},
	confirm: function(message, option) {
		return $.confirm({
			title: '<strong style="color: #c7254e;font-size: 16px">温馨提示</strong>',
			content: '<div class="text-center" style="border-top:1px solid #eee;padding-top: 20px">' + message + '</div>',
			confirmButton: '确定',
			confirmButtonClass: 'btn btn-info',
			cancelButton: '取消',
			cancelButtonClass: 'btn btn-danger',
			animation: 'scaleY',
			theme: 'material',
			confirm: function() {
				if (option) {
					if (option.toUrl) {
						window.location.href = option.toUrl
					} else if (option.callback) {
						let event = option.event
						option.callback.call(event, event)
					} else if (!option.stay) {
						window.location.reload()
					}
				}
			}
		});
	},
	ajaxPost: function(url, data, event, callback) {
		let urls = url ? url : '';
		$.ajax({
			url: urls,
			method: 'POST',
			data: data,
			dataType: 'json',
			success: function(json) {
				if (json.code !== 0) {
					main.alert(json.msg)
				} else {
					let dialog = null;
					if (json.msg !== 'success') dialog = main.alert(json.msg, $.extend(json, {event:event, callback:callback}))
					setTimeout(function(){
						if (json.stay) {
							if (dialog) dialog.close();
						}
						if (json.toUrl) {
							window.location.href = json.toUrl
						} else if (callback) {
							callback.call(event, event, json)
						} else if (!json.stay) {
							window.location.reload()
						}
					}, json.msg !== 'success' ? json.msg.replace(/[^\x00-\xff]/g, '01').length * 150 : 0)
				}
			}
		});
	},
	ajaxForm: function(event, callback) {
		let obj = $(event);
		let url = obj.attr('data-url');
		let data = obj.parents('form').serialize();
		obj.attr('disabled', true);
		main.ajaxPost(url, data, event, callback);
		obj.attr('disabled', false);
	},
	ajaxDelete: function(event, callback) {
		let message = $(event).attr('data-msg');
		let url = $(event).attr('data-url');
		main.confirm(message, {
			event: event,
			callback: function() {
				main.ajaxPost(url, null, event, callback)
			}
		});
	},
	//模态弹窗
	//{ name:'name', type:'select', value:'value', height:80, require:true, msg:'请填写', placeholder:'placeholder', item:[{value:'value', text:'text', checked:true, selected:true}] },
	modalView: function(event, option) {
		if (event && !(event instanceof jQuery) && Object.keys(JSON.parse(JSON.stringify(event))).length) {
			option = event;
			event = null;
		}
		option = option || $(event).attr('data-option') || null;
		if (typeof option === 'string') option = JSON.parse(option);
		if (!option) {
			main.alert('模态插件缺少参数')
			return;
		}
		let url = option.url || $(event).attr('data-url') || null;
		if (!url) {
			main.alert('模态插件缺少请求网址')
			return;
		}
		//option.callback自定义回调处理，option.post自动提交(function提交后回调|true不回调)
		if (!option.callback && !option.post) {
			main.alert('模态插件缺少callback函数或post函数')
			return;
		}
		let content = '';
		if (option.item) {
			content += '<div class="form-horizontal"  style="padding-top:10px;">';
			for (let i = 0; i < option.item.length; i++) {
				let g = option.item[i]
				if (typeof g.name === 'undefined') continue
				if (typeof g.type === 'undefined') g.type = 'text'
				let name = g.name
				content += '<div class="form-group"><div class="col-sm-12">'
				if (g.type === 'select') {
					content += '<select class="form-control modal_select_'+name+'">'
					if (g.placeholder) content += '<option value="">'+g.placeholder+'</option>'
					if (g.item) {
						for (let k = 0; k < g.item.length; k++) {
							content += '<option value="'+g.item[k].value+'" '+(g.item[k].selected?'selected':'')+'>'+g.item[k].text+'</option>'
						}
					}
					content += '</select>'
				} else if (g.type === 'textarea') {
					let height = g.height ? g.height : 80
					content += '<textarea class="form-control modal_textarea_'+name+'" style="height:'+height+'px" placeholder="'+(g.placeholder?g.placeholder:'')+'">'+(g.value?g.value:'')+'</textarea>'
				} else if (g.type === 'radio') {
					if (g.item) {
						for (let k = 0; k < g.item.length; k++) {
							content += '<label class="radio radio-custom pull-left" style="padding-top:0;"><input type="radio" class="modal_radio_'+name+'" name="modal_radio_'+name+'" value="'+g.item[k].value+'" '+(g.item[k].checked?'checked':'')+'><i></i> '+g.item[k].text+'</label>'
						}
					}
				} else if (g.type === 'checkbox') {
					if (g.item) {
						for (let k = 0; k < g.item.length; k++) {
							content += '<label class="checkbox checkbox-custom pull-left" style="padding-top:0;"><input type="checkbox" class="modal_checkbox_'+name+'" name="modal_checkbox_'+name+'" value="'+g.item[k].value+'" '+(g.item[k].checked?'checked':'')+'><i></i> '+g.item[k].text+'</label>'
						}
					}
				} else if (g.type === 'number') {
					content += '<input type="number" class="form-control modal_number_'+name+'" value="'+(g.value?g.value:'')+'" placeholder="'+(g.placeholder?g.placeholder:'')+'">'
				} else if (g.type === 'password') {
					content += '<input type="password" class="form-control modal_password_'+name+'" value="'+(g.value?g.value:'')+'" placeholder="'+(g.placeholder?g.placeholder:'')+'">'
				} else if (g.type === 'hidden') {
					content += '<input type="hidden" value="'+(g.value?g.value:'')+'">'
				} else {
					content += '<input type="text" class="form-control modal_input_'+name+'" value="'+(g.value?g.value:'')+'" placeholder="'+(g.placeholder?g.placeholder:'')+'">'
				}
				content += '</div></div>'
			}
			content += '</div>';
		}
		if (!content.length) {
			main.alert('模态插件缺少内容')
			return;
		}
		$.confirm({
			title: option.title ? '<strong style="color: #c7254e;font-size: 16px">'+option.title+'</strong>' : '',
			content: content,
			confirmButton: '确定',
			confirmButtonClass: 'btn btn-info',
			cancelButton: '取消',
			cancelButtonClass: 'btn btn-danger',
			animation: 'scaleY',
			theme: 'material',
			confirm: function() {
				if (option.item) {
					let msg = '', feasible = true, data = {}, callback = null
					for (let i = 0; i < option.item.length; i++) {
						let g = option.item[i], item, value = ''
						if (typeof g.name === 'undefined') continue
						if (g.type === 'select') {
							item = $('.modal_select_'+g.name)
							value = item.find('option:selected').val()
							if (g.require && !value.length) {
								msg = g.msg ? g.msg : '请选择'
								feasible = false
								item.focus()
								break
							}
						} else if (g.type === 'radio') {
							item = $('.modal_radio_'+g.name)
							let checked = item.filter(':checked')
							if (g.require && !checked.length) {
								msg = g.msg ? g.msg : '请选择'
								feasible = false
								callback = {callback:function(){
									item.parent().addClass('radio-scale')
									setTimeout(function(){item.parent().removeClass('radio-scale')}, 1500)
								}}
								break
							}
							value = checked.val()
						} else if (g.type === 'checkbox') {
							item = $('.modal_checkbox_'+g.name)
							let checked = item.filter(':checked')
							if (g.require && !checked.length) {
								msg = g.msg ? g.msg : '请选择'
								feasible = false
								callback = {callback:function(){
									item.parent().addClass('checkbox-scale')
									setTimeout(function(){item.parent().removeClass('checkbox-scale')}, 1500)
								}}
								break
							}
							value = checked.val()
						} else if (g.type === 'textarea') {
							item = $('.modal_textarea_'+g.name)
							value = item.val()
							if (g.require && !value.length) {
								msg = g.msg ? g.msg : '请输入'
								feasible = false
								item.focus()
								break
							}
						} else if (g.type === 'number') {
							item = $('.modal_number_'+g.name)
							value = item.val()
							if (g.require && !value.length) {
								msg = g.msg ? g.msg : '请输入数字'
								feasible = false
								item.focus()
								break
							}
						} else if (g.type === 'password') {
							item = $('.modal_password_'+g.name)
							value = item.val()
							if (g.require && !value.length) {
								msg = g.msg ? g.msg : '请输入密码'
								feasible = false
								item.focus()
								break
							}
						} else {
							item = $('.modal_input_'+g.name)
							value = item.val()
							if (g.require && !value.length) {
								msg = g.msg ? g.msg : '请填写'
								feasible = false
								item.focus()
								break
							}
						}
						data[g.name] = value
					}
					if (!feasible) {
						main.alert(msg, callback)
						return false
					}
					if (option.post) {
						main.ajaxPost(url, data, event, $.isFunction(option.post) ? option.post : null)
					} else {
						option.callback.call(event, event, data)
					}
				}
			}
		});
	},
	//初始化上传控件
	setUploader: function(cls) {
		$(cls).not('[data-configs]').attr('data-configs', 'init').each(function(){
			let _this = $(this), url = _this.attr('data-url');
			if (!url) {
				main.alert('setUploader 缺少自定义属性参数');
				return true;
			}
			let html5upload = _this.attr('data-html5upload') ? _this.attr('data-html5upload') : null,
				dir = _this.attr('data-dir') ? _this.attr('data-dir') : null,
				name = _this.attr('data-name') ? _this.attr('data-name') : null,
				splitSize = _this.attr('data-splitSize') ? parseInt(_this.attr('data-splitSize')) : 0,
				data = _this.attr('data-data') ? _this.attr('data-data') : null,
				fileType = _this.attr('data-fileType') ? _this.attr('data-fileType') : '',
				text = _this.attr('data-text') ? _this.attr('data-text') : (fileType ? '选择文件' : '选择图片'),
				height = _this.attr('data-height') ? _this.attr('data-height') : 24,
				multiple = $.inArray(_this.attr('data-multiple'),['true','false'])>-1 ? eval(_this.attr('data-multiple')) : false,
				className = _this.attr('data-class') ? _this.attr('data-class') : '',
				attr = _this.attr('data-attr') ? _this.attr('data-attr').replace(/`/, '"') : '',
				item = _this.attr('data-item') ? _this.attr('data-item') : null,
				thumb = _this.attr('data-thumb') ? ($.inArray(_this.attr('data-thumb'),['true','false'])>-1 ? eval(_this.attr('data-thumb')) : false) : '',
				callback = _this.attr('data-callback') ? _this.attr('data-callback') : null;
			let html = '<div style="float:left;">'+text+'</div><div style="float:left;"></div>';
			_this.html(html).css('float', 'left');
			if (data) {
				if(/\b(\w+)\s*:/.test(data) || /\b'(\w+)'\s*:/.test(data)){
					data = data.replace(/\b(\w+)\s*:/g, '"$1":').replace(/\b'(\w+)'\s*:/g, '"$1":').replace(/^\[\s*,/, '[').replace(/,\s*]$/, ']');
					data = data.replace(/:\s*([^[{]+?|'.+?'|".+?"|`.+?`)(\s*,|\s*}|$)/g, function(_$, $1, $2){
						if(!isNaN($1))return _$;
						if(/^"/.test($1))return _$;
						if(/^`(.+)`$/.test($1))return ':"'+$1.replace(/^`(.+)`$/, '$1').replace(/"/g, '\"')+'"'+$2;
						if(/^'(.+)'$/.test($1))return ':"'+$1.replace(/^'(.+)'$/, '$1').replace(/"/g, '\"')+'"'+$2;
						if(/^''$/.test($1))return ':""'+$2;
						return ':"'+$1.replace(/"/g, '\"')+'"'+$2;
					});
					if($.isJsonString(data))data = JSON.parse(data);
				}else if(/^\w+$/.test(data) || /^function/.test(data)){
					let fn = eval(data);
					if($.isFunction(fn))data = fn.call(_this);
				}
			}
			if (fileType) data = !data ? { suffix:fileType } : $.extend(data, { suffix:fileType });
			if (dir) data = !data ? { dir:dir } : $.extend(data, { dir:dir });
			if ( (typeof thumb === 'boolean' && thumb) || (typeof thumb === 'string' && !thumb.length) ) {
				thumb = '<div class="file-item thumbnail draggable-element"'+((!fileType || fileType.indexOf('jpg')>-1) ? '' : ' style="display:none;"')+'>' +
					'<a href="'+'%s" target="_blank"><img src="%s" style="height:'+height+'px;"></a>' +
					'<a class="file-panel" href="javascript:void(0)" onclick="$(this).parent().remove()"><span class="fa fa-close"></span></a>' +
					'<input type="hidden" '+(className?'class="'+className+'"':'')+' '+(name?'name="'+name+'"':'')+' value="%s" '+(attr?attr:'')+' />' +
					'</div>';
			}
			let children = _this.children();
			if (html5upload) {
				children.eq(0).html('<div class="btn btn-primary" style="position:relative;cursor:pointer;">'+text+'<input type="file" style="position:absolute;left:0;top:0;right:0;bottom:0;opacity:0;cursor:pointer;" /></div>');
				children.eq(0).find('input').html5upload({
					url: url,
					//name: name ? name : 'file',
					data: data,
					fileType: fileType ? fileType : ['jpg', 'jpeg', 'png', 'gif', 'bmp'],
					splitSize: splitSize,
					before: function(){
						$.overload();
					},
					success: function(res){
						$.overload(false);
						if (res.code !== 0) {
							main.alert(res.msg);
							return;
						}
						let src = res.data;
						if (typeof thumb === 'string' && thumb.length) {
							let thumber = $(thumb.replace(/`/, '"').replace(/%s/g, src)).css({'margin-left':'10px', float:'left'});
							if (!thumber.find('[type="hidden"]').length) {
								thumber.wrap('<div></div>');
								thumber = thumber.parent();
								thumber.append('<input type="hidden" '+(className?'class="'+className+'"':'')+' '+(name?'name="'+name+'"':'')+' value="'+src+'" '+(attr?attr:'')+' />');
							}
							if (multiple) {
								children.eq(1).append(thumber);
							} else {
								children.eq(1).html(thumber);
							}
						}
						if(callback){
							let func = eval(callback);
							if($.isFunction(func))func.call(_this, src);
						}
					},
					error: function(){
						$.overload(false);
					}
				});
				if (typeof thumb === 'string' && thumb.length && item) {
					if (typeof item === 'string' && item.length) item = item.split(',');
					if ($.isArray(item)) {
						for (let i in item) {
							if (item[i] && item[i].length) {
								let thumber = $(thumb.replace(/`/, '"').replace(/%s/g, item[i])).css({'margin-left':'10px', float:'left'});
								if (!thumber.find('[type="hidden"]').length) {
									thumber.wrap('<div></div>');
									thumber = thumber.parent();
									thumber.append('<input type="hidden" '+(className?'class="'+className+'"':'')+' '+(name?'name="'+name+'"':'')+' value="'+item[i]+'" '+(attr?attr:'')+' />');
								}
								children.eq(1).append(thumber);
							}
						}
					}
				}
				return true;
			}
			main.uploader({
				picker: children.eq(0),
				container: children.eq(1),
				url: url,
				dir: dir,
				name: name,
				data: data,
				multiple: multiple,
				item: item,
				thumb: thumb,
				callback: callback
			});
		});
	},
	uploader: function(options) {
		let pickerId = options.picker, containerId = options.container, targetUrl = options.url, dir = options.dir, name = options.name, data = options.data||null,
			multiple = options.multiple||false, item = options.item, thumb = options.thumb, callback = options.callback;
		if (dir) data = !data ? { dir:dir } : $.extend(data, { dir:dir });
		//https://www.hangge.com/blog/cache/detail_2269.html
		let uploader = WebUploader.create({
			auto: true, //选择文件后，是否自动上传
			duplicate: true, //去重
			swf: '/static/js/webuploader/Uploader.swf', //swf文件路径 换成你的接收路径
			server: targetUrl, //文件接收服务端 换成你的接收路径
			pick: {id:$(pickerId), multiple:multiple}, //选择文件的按钮
			//fileVal: name ? name : 'file', //提交文件二进制的name
			formData: data, //附加提交数据
			onUploadSuccess: function(file, res) {
				if (res.code !== 0) {
					main.alert(res.msg);
					return;
				}
				let src = res.data;
				if (typeof thumb === 'string' && thumb.length) {
					let thumber = $(thumb.replace(/`/, '"').replace(/%s/g, src)).css({'margin-left':'10px', float:'left'});
					if (!thumber.find('[type="hidden"]').length) {
						thumber.wrap('<div></div>');
						thumber = thumber.parent();
						thumber.append('<input type="hidden" '+(className?'class="'+className+'"':'')+' '+(name?'name="'+name+'"':'')+' value="'+src+'" '+(attr?attr:'')+' />');
					}
					if (multiple) {
						$(containerId).append(thumber);
					} else {
						$(containerId).html(thumber);
					}
				}
				if(callback){
					let func = eval(callback);
					if($.isFunction(func))func(src);
				}
				/*uploader.makeThumb(file, function(error, src) { //创建缩略图 如果为非图片文件，可以不用调用此方法 100（宽） x 100（高）
					if (error) {
						img.replaceWith('<span>不能预览</span>');
						return;
					}
					img.attr('src', src);
				}, 100, 100 );*/
			}
		});
		uploader.on('fileQueued', function(file) {}); //当有文件添加进来的时候
		uploader.on('uploadError', function(file) {}); //文件上传失败，显示上传出错
		if (typeof thumb === 'string' && thumb.length && item) {
			if (typeof item === 'string' && item.length) item = item.split(',');
			if ($.isArray(item)) {
				for (let i in item) {
					if (item[i] && item[i].length) {
						let thumber = $(thumb.replace(/`/, '"').replace(/%s/g, item[i])).css({'margin-left':'10px', float:'left'});
						if (!thumber.find('[type="hidden"]').length) {
							thumber.wrap('<div></div>');
							thumber = thumber.parent();
							thumber.append('<input type="hidden" '+(className?'class="'+className+'"':'')+' '+(name?'name="'+name+'"':'')+' value="'+item[i]+'" '+(attr?attr:'')+' />');
						}
						$(containerId).append(thumber);
					}
				}
			}
		}
	},
	//新建ckeditor，同页面多编辑器用
	setEditor: function(expr, dir, setSimple) {
		let element = $(expr);
		if (!element.length) return;
		let height = element.outerHeight(false);
		if (height < 300) height = 300;
		element.wrap('<div class="ck-editor__wrap"></div>');
		let isInput = element.is(':text, textarea');
		let full = [
			'SourceEditing',
			'|','Undo','Redo',
			'|','Heading',
			'|','FindandReplace','SelectAll','RemoveFormat','Bold','Italic','Underline','Strikethrough','|','Subscript','Superscript','Alignment','Code',
			'|','FontFamily','FontSize','FontColor','FontBackgroundColor',/*'Highlight',*/
			'|','NumberedList','BulletedList','TodoList','Outdent','Indent','BlockQuote',
			'|','Link','InsertImage','MediaEmbed','InsertTable','HorizontalLine','SpecialCharacters','PageBreak'
		];
		let simple = [
			'SourceEditing',
			'|','Bold','Italic','Underline','Strikethrough','|','Alignment','RemoveFormat',
			'|','FontFamily','FontSize','FontColor','FontBackgroundColor',
			'|','Link','InsertImage'
		];
		let config = $.extend(window.ckeditorConfig, {
			toolbar: {
				items: setSimple ? simple : full,
				shouldNotGroupWhenFull: true //多行
			},
			ckfinder: {
				uploadUrl: '/'+MODULE_NAME+'/upload/editor' + (dir ? '?dir='+dir : '')
				//wechatCollectUrl: '/'+MODULE_NAME+'/upload/ckediter_wechat_collect' + (dir ? '?dir='+dir : '')
				/*服务器返回: {
					uploaded: 1, //是否上传成功
					url: '/' //图片网址
				}*/
			},
			language: 'zh-cn'
		});
		ClassicEditor.create(element[0], config).then(editor => {
			if (typeof window.editor === 'undefined') {
				window['editor'+expr] = window.editor = editor;
			} else {
				window['editor'+expr] = editor;
			}
			editor.editing.view.change(() => {
				element.next().find('.ck-editor__editable_inline').addClass('ck-editor-for-'+element.attr('name')).parent().height(height);
			});
			editor.model.document.on('change:data', function() {
				if (isInput) {
					element.val(editor.getData());
				} else {
					element.html(editor.getData());
				}
			});
			/*let wordCountPlugin = editor.plugins.get('WordCount');
			document.getElementById('word-count').appendChild(wordCountPlugin.wordCountContainer);
			wordCountPlugin.on('update', (evt, stats) => {
				console.log(`Characters: ${stats.characters}\nWords: ${stats.words}`);
			});*/
		}).catch(error => {
			console.error('There was a problem initializing the editor.', error);
		});
	},
	//弹窗操作
	iframeLayer: function(url, title, width, height, scrolling, max){
		let showLayerArea = window.document;
		if(typeof url === 'boolean' && !url){
			let layer = $('.col-layer:last', showLayerArea);
			layer.addClass('col-layer-toggle');
			setTimeout(function(){
				layer.remove();
				if(!$('.col-layer', showLayerArea).length){
					let layerShadow = $('.col-layer-shadow', showLayerArea);
					layerShadow.addClass('col-layer-shadow-toggle');
					setTimeout(function(){layerShadow.remove()}, 310);
				}
			}, 310);
			return;
		}
		let html = '', win = $.window(), classUrl = $.base64().encode(url).replace(/[=/]/g, ''), hasShadow = true;
		if(typeof scrolling === 'undefined')scrolling = '';
		if(!$('.col-layer-shadow', showLayerArea).length){
			hasShadow = false;
			html += '<div class="col-layer-shadow col-layer-shadow-toggle"></div>';
		}else{
			$('.col-layer-shadow', showLayerArea).show();
		}
		html += '<div class="col-layer col-layer-normal iframe-layer-'+$.base64().encode('/'+MODULE_NAME+'/login').replace(/[=/]/g, '')+' iframe-layer-'+classUrl+'" style="opacity:0;'+(!!width?'width:'+width+'px;':'')+(!!height?'height:auto;':'')+'">\
			<div class="layer-header page-header">\
				<a href="javascript:void(0)"></a>\
				<a href="javascript:void(0)"></a>\
				<a href="javascript:void(0)"></a>\
				<h6>'+((!!title&&title.length)?title:'')+'</h6>\
			</div>\
			<div class="layer-content" style="'+(!!height?'height:'+height+'px;':'')+'">\
				<div class="db-circle-ico"></div>\
				<iframe name="iframe-layer-case-'+classUrl+'" src="'+url+'" frameborder="0" '+scrolling+'></iframe>\
			</div>\
		</div>';
		$('body', showLayerArea).append(html);
		let layer = $('.col-layer:last', showLayerArea), layerShadow = $('.col-layer-shadow', showLayerArea),
			left = (win.width-layer.width()) / 2, top = (win.height-layer.height()) / 2;
		layer.css({left:left, top:top, opacity:''}).addClass('col-layer-toggle').attr({'origin-left':left, 'origin-top':top, 'origin-width':layer.width(), 'origin-height':layer.find('.layer-content').height()});
		setTimeout(function(){
			layerShadow.removeClass('col-layer-shadow-toggle');
			layer.removeClass('col-layer-toggle');
		}, 100);
		if(!hasShadow)layerShadow.on('click', function(){
			let allLayer = $('.col-layer', showLayerArea);
			allLayer.addClass('col-layer-toggle');
			setTimeout(function(){
				allLayer.remove();
				if(!$('.col-layer', showLayerArea).length){
					layerShadow.addClass('col-layer-shadow-toggle');
					setTimeout(function(){layerShadow.remove()}, 310);
				}
			}, 310);
		});
		layer.find('.layer-header').drag({
			target: layer,
			area: $('body', showLayerArea),
			exceptEl: function(e){
				let parent = this.parent();
				if(parent.hasClass('col-layer-max') || parent.hasClass('col-layer-min') || $(e.target).is('a'))return true;
			},
			stop: function(position){
				this.attr({'origin-left':position.left, 'origin-top':position.top});
			}
		});
		layer.find('.layer-header > a').on('click', function(){
			let index = $(this).index();
			if(index === 0){
				layer.addClass('col-layer-toggle');
				let layerCount = $('.col-layer', showLayerArea).length;
				setTimeout(function(){
					layer.remove();
					if(layerCount > 1)return;
					layerShadow.addClass('col-layer-shadow-toggle');
					setTimeout(function(){layerShadow.remove()}, 310);
				}, 310);
			}else if(index === 1){
				layer.css({left:'', top:'', bottom:''});
				if(layer.is('.col-layer-min')){
					layer.addClass('col-layer-normal').removeClass('col-layer-min').css({
						left:layer.attr('origin-left')+'px', top:layer.attr('origin-top')+'px', width:layer.attr('origin-width'), height:!!height ? 'auto' : ''
					});
					let minLeft = 10, minBottom = 10;
					$('.col-layer-min', showLayerArea).each(function(){
						let minBox = $(this).css({left:minLeft, bottom:minBottom});
						let minWidth = minBox.width(), offset = minBox.offset();
						if(offset.left+minWidth+10+minWidth > win.width){
							minLeft = 10;
							minBottom = win.height - offset.top + 10;
						}else{
							minLeft = offset.left + minWidth + 10;
						}
					});
					setTimeout(function(){
						layer.css({'-webkit-transition':'', 'transition':''});
					}, 10);
					layerShadow.show();
				}else{
					if(layer.is('.col-layer-max')){
						layer.addClass('col-layer-normal').removeClass('col-layer-max').css({
							left:layer.attr('origin-left')+'px', top:layer.attr('origin-top')+'px', width:layer.attr('origin-width'), height:!!height ? 'auto' : ''
						}).find('.layer-content').animate({height:layer.attr('origin-height')}, 150);
					}else{
						layer.removeClass('col-layer-normal').addClass('col-layer-max').css({left:'', top:'', width:'', height:''}).find('.layer-content').css({height:''});
					}
				}
			}else if(index===2){
				layer.css({'-webkit-transition':'none', 'transition':'none'}).removeClass('col-layer-normal').removeClass('col-layer-max').css({left:'', top:'', bottom:'', width:'', height:''});
				let minLeft = 10, minBottom = 10;
				$('.col-layer-min', showLayerArea).each(function(){
					let minBox = $(this).css({left:minLeft, bottom:minBottom});
					let minWidth = minBox.width(), offset = minBox.offset();
					if(offset.left + minWidth + 10 + minWidth > win.width){
						minLeft = 10;
						minBottom = win.height - offset.top + 10;
					}else{
						minLeft = offset.left + minWidth + 10;
					}
				});
				layer.addClass('col-layer-min').css({left:minLeft, bottom:minBottom});
				if(!$('.col-layer-normal', showLayerArea).length)layerShadow.hide();
			}
		});
		if(!!max)layer.find('.layer-header a:eq(1)').trigger('click');
	},
};
