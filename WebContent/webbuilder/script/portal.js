var Pt = {
	load : function(panel, reload) {
		if (panel && panel.saveUrl && (reload || !panel.moduleLoaded)) {
			var id = panel.iframeId, o = Wb.dom(id);
			o.style.visibility = 'hidden';
			Wb.submit(panel.saveUrl, panel.params, id, panel.openType);
			o.style.visibility = 'visible';
			panel.moduleLoaded = true;
		}
	},
	moduleTabChange : function(sender, newCard) {
		if (newCard == homePage)
			Pt.openHome();
		else
			Pt.load(newCard);
	},
	run : function(r) {
		if (!r.get('IS_FOLDER')) {
			Pt.savePath();
			var u = 'main?xwl=' + r.get('MODULE_ID');
			if (r.get('NEW_WIN'))
				window.open(u);
			else
				WBXwlOpen(u, r.get('text'), r.get('iconCls'));
		}
	},
	viewConfig : function() {
		return {
			plugins : {
				pluginId : 'ddPlug',
				ptype : 'treeviewdragdrop',
				ddGroup : 'portal',
				enableDrop : false
			}
		}
	},
	savePath : function() {
		if (Pt.pathSaving || Pt.indexPath === '-')
			return;
		var n = Wb.getSelNode(moduleTree), p;
		if (n) {
			p = n.getPath('MODULE_ID', '\n');
			if (p === Pt.savedPath)
				return;
			Pt.savedPath = p;
			Pt.pathSaving = 1;
			Ext.Ajax.request( {
				url : 'main?xwl=13NOEQY1P3LO',
				params : {
					path : p
				},
				callback : function() {
					delete Pt.pathSaving;
				}
			});
		}
	},
	open : function(tab, url, title, iconCls, params, type, x, y, w, h) {
		if (params == null && x === undefined) {
			var b = false;
			tab.items.each(function(p) {
				if (p.saveUrl === url) {
					tab.setActiveTab(p);
					b = true;
					return false;
				}
			});
			if (b)
				return false;
		}
		var ti = 'xi_' + Wb.getId(), obj, t;
		obj = {
			iconCls : iconCls,
			saveUrl : url,
			iframeId : ti,
			layout : 'fit',
			params : params,
			hideMode : 'offsets',
			openType : type,
			title : Wb.ellipsis(title),
			orgTitle : title,
			listeners : {
				render : function(t) {
					if (t.title !== t.orgTitle)
						t.ellipsisTip = new Ext.tip.ToolTip( {
							target : t.tab.btnWrap,
							html : title
						});
				},
				beforeclose : function(t) {
					var f, r, w = Wb.dom(t.iframeId).contentWindow;
					if (!w.wb_forceCls) {
						f = w.wb_beforeunload;
						r = f ? f() : null;
						if (r !== null && r !== undefined) {
							Wb.confirm(r + '<br>' + Str.closeConfirm,
									function() {
										w.wb_forceCls = true;
										t.close();
									});
							return false;
						}
					}
				},
				beforedestroy : function(t) {
					try {
						var id = t.iframeId, f = Wb.dom(id), o = f.contentWindow.document
								|| f.contentDocument
								|| window.frames[id].document;
						f.src = '';
						o.write('');
						o.close();
						Ext.fly(f).destroy();
						Wb.closeNav(backBtn, forwardBtn, t);
					} catch (e) {
					}
				}
			},
			closable : true,
			html : '<iframe id="'
					+ ti
					+ '" name="'
					+ ti
					+ '" scrolling="auto" frameborder="0" width="100%" height="100%"></iframe>'
		};
		if (x === undefined)
			t = tab.add(obj);
		else {
			Ext.apply(obj, {
				x : x < 0 ? 0 : x,
				y : y < 0 ? 0 : y,
				width : w || 380,
				height : h || 260,
				liveDrag : true,
				autoShow : true,
				floating : false,
				bodyStyle : 'background-image:none;background-color:#FFFFFF',
				resizable : {
					listeners : {
						beforeresize : function() {
							var me = this;
							if (!me.setMouseUp) {
								me.setMouseUp = true;
								me.resizeTracker.on( {
									mouseup : function() {
										Ext.fly(Wb.dom(ti)).removeCls(
												'x-hide-offsets');
									}
								});
							}
							Ext.fly(Wb.dom(ti)).addCls('x-hide-offsets');
						}
					}
				},
				draggable : {
					listeners : {
						dragstart : function() {
							Ext.fly(Wb.dom(ti)).addCls('x-hide-offsets');
						},
						dragend : function() {
							Ext.fly(Wb.dom(ti)).removeCls('x-hide-offsets');
						}
					}
				},
				tools : [
						{
							type : 'refresh',
							handler : function() {
								Pt.load(t, true);
							}
						},
						{
							type : 'maximize',
							handler : function() {
								moduleTab.setActiveTab(Pt.open(moduleTab,
										t.saveUrl, t.orgTitle, t.iconCls));
							}
						} ],
				xtype : 'window'
			});
			t = tab.add(obj);
		}
		return t;
	},
	logout : function() {
		var f, b = false;
		homePage.items.each(function(c) {
			f = Wb.dom(c.iframeId).contentWindow.wb_beforeunload;
			if (f) {
				f = f();
				if (!b && f !== null && f !== undefined) {
					moduleTab.setActiveTab(homePage);
					b = true;
				}
			}
		});
		moduleTab.items.each(function(c) {
			if (c != homePage) {
				f = Wb.dom(c.iframeId).contentWindow.wb_beforeunload;
				if (f) {
					f = f();
					if (!b && f !== null && f !== undefined) {
						moduleTab.setActiveTab(c);
						b = true;
					}
				}
			}
		});
		function doLogout() {
			Wb.request( {
				url : 'main?xwl=logout',
				success : function() {
					Pt.canLogout = true;
					window.location = 'main';
				}
			});
		}
		if (b)
			Wb.confirm(Str.closeConfirm, doLogout);
		else
			doLogout();

	},
	close : function(f) {
		var o, t = moduleTab;
		o = t.getActiveTab();
		t.items.each(function(p) {
			if (p != homePage && (f || o != p))
				p.close();
		});
	},
	monDbClick : function() {
		var t = moduleTab.getActiveTab();
		if (t.saveUrl && !t.params)
			window.open(t.saveUrl);
	},
	ddSupport : function(panel) {
		new Ext.dd.DropTarget(panel.body.dom, {
			ddGroup : 'portal',
			notifyDrop : function(ddSource, e, data) {
				var x = e.getX() - panel.body.getLeft(), y = e.getY()
						- panel.body.getTop(), n = data.records[0];
				if (!n.get('IS_FOLDER')) {
					Pt.load(Pt.open(panel, 'main?xwl=' + n.get('MODULE_ID'), n
							.get('text'), n.get('iconCls'), null, null, x, y));
					return true;
				}
			}
		});
	},
	getDesktop : function() {
		var i, j = moduleTab.items.length, t, x = [], r;
		r = [ {
			width : moduleTree.getWidth(),
			index : moduleTab.items.indexOf(moduleTab.getActiveTab())
		} ];
		j = homePage.items.length;
		for (i = 0; i < j; i++) {
			t = homePage.items.items[i];
			if (t.saveUrl)
				x.push( {
					url : t.saveUrl,
					title : t.orgTitle,
					icon : t.iconCls,
					x : t.x,
					y : t.y,
					w : t.width,
					h : t.height
				});
		}
		r.push(x);
		j = moduleTab.items.length;
		for (i = 1; i < j; i++) {
			t = moduleTab.items.items[i];
			r.push( {
				url : t.saveUrl,
				title : t.title,
				icon : t.iconCls
			});
		}
		return Ext.encode(r);
	},
	initialize : function() {
		Wd.WBXwlOpen = function(url, title, iconCls, params, type) {
			moduleTab.setActiveTab(Pt.open(moduleTab, url, title, iconCls,
					params, type));
		}
	},
	finalize : function() {
		if (desktopData != null) {
			var d = desktopData, x = d[1], i, j, o;
			moduleTree.setWidth(d[0].width);
			j = x.length;
			for (i = 0; i < j; i++) {
				o = x[i];
				Pt.open(homePage, o.url, o.title, o.icon, null, null, o.x, o.y,
						o.w, o.h);
			}
			j = d.length;
			for (i = 2; i < j; i++) {
				o = d[i];
				Pt.open(moduleTab, o.url, o.title, o.icon);
			}
			i = d[0].index;
			if (i == 0)
				Pt.openHome();
			else
				moduleTab.setActiveTab(i);
		}
	},
	openHome : function() {
		var x = homePage.items, i, j = x.length;
		for (i = 0; i < j; i++)
			Pt.load(x.items[i]);
	},
	refresh : function() {
		var t = moduleTab.getActiveTab(), i, j, x;
		if (t == homePage) {
			x = homePage.items;
			j = x.length;
			for (i = 0; i < j; i++)
				Pt.load(x.items[i], true);
		} else
			Pt.load(t, true);
	}
};