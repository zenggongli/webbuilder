var Ds = {
	clipBoard : null,
	stopSetProp : false,
	stopRecChg : false,
	stopSync : false,
	ctrlKey : false,
	consoleTimer : null,
	keyCode : 0,
	moduleSeled : true,
	isFolder : false,
	isInsert : false,
	consoleIdx : 0,
	consoleRaws : [],
	isCut : false,
	copiedModule : null,
	id : 1,
	at : function() {
		return moduleTab.getActiveTab();
	},
	getId : function() {
		return 'ds_' + Ds.id++;
	},
	objectTreeFields : [ 'text', 'id', 'xwlMeta', 'xwlChildren', 'xwlParent',
			'xwlXtype', 'xwlCategory', 'xwlWidth', 'xwlHeight', 'xwlMinWidth',
			'xwlMinHeight', 'xwlMaxWidth', 'xwlMaxHeight', 'xwlPT',
			'properties', 'events', 'custPEditors', 'custEEditors', 'custEPara' ],
	moduleTreeFields : [ 'text', 'orgText', 'iconCls', 'MODULE_ID',
			'PARENT_ID', 'IS_HIDDEN', 'NEW_WIN', 'IS_FOLDER', 'CREATE_USER',
			'CREATE_DATE', 'LAST_MODIFY_USER', 'LAST_MODIFY_DATE',
			'ORDER_INDEX' ],
	createTab : function(title, id, iconCls, callback) {
		var tab = moduleTab, pageId = 't_' + id, page;
		if (Wb.get(pageId)) {
			tab.setActiveTab(pageId);
			if (callback)
				callback(Wb.get(pageId).objectTree, Wb.get(pageId));
			return;
		}
		function destroyCmp(r) {
			var o, m, x;
			r.eachChild(function(c) {
				m = c.custPObjs;
				for (o in m) {
					x = m[o];
					if (Ext.isObject(x))
						x.destroy();
				}
				m = c.custEObjs;
				for (o in m) {
					x = m[o];
					if (Ext.isObject(x))
						x.destroy();
				}
				destroyCmp(c);
			});
		}

		function loadData(r) {
			var n = page.objectTree.selectObject, p = page.propertyGrid, e = page.eventGrid, f = p.store.sorters.items[0], x = p.view.el.dom.scrollTop, y = e.view.el.dom.scrollTop;
			p.setSource(n.get('properties'));
			p.store.sort(f.property, f.direction);
			if (!r.custPObjs)
				r.custPObjs = Ext.apply( {}, r.get('custPEditors'));
			p.plugins[0].editors.map = r.custPObjs;
			p.view.el.dom.scrollTop = x;
			f = e.store.sorters.items[0];
			e.setSource(n.get('events'));
			e.store.sort(f.property, f.direction);
			if (!r.custEObjs)
				r.custEObjs = Ext.apply( {}, r.get('custEEditors'));
			e.plugins[0].editors.map = r.custEObjs;
			e.view.el.dom.scrollTop = y;
		}

		function blockModify(a, b, s) {
			var c = s.plugins[0].editors.map[b.record.get('name')]
			if (c && c.field) {
				c.field.allowChange = false;
				if (c.field.blockPost)
					return false;
			}
			return true;
		}
		tab
				.add( {
					id : pageId,
					iconCls : iconCls,
					title : Wb.ellipsis(title),
					orgTitle : title,
					closable : true,
					layout : 'fit',
					xtype : 'tabpanel',
					plugins : [ {
						ptype : 'tabscrollermenu'
					} ],
					deferredRender : false,
					listeners : {
						tabchange : function(a, b, c) {
							Ds.rsPos(1);
							Wb.recNav(b, c, backBtn);
						},
						beforetabchange : function(t, n, o) {
							Ds.svPos(1);
							Ds.completeEdit();
							if (o && o.commitChange)
								o.commitChange();
						},
						render : function(t) {
							page = t;
							if (t.title !== t.orgTitle)
								t.ellipsisTip = new Ext.tip.ToolTip( {
									target : t.tab.btnWrap,
									html : title
								});
						},
						activate : function(t) {
							saveBtn.setDisabled(!t.isModified);
							if (!t.saveUrl) {
								var u = t.getActiveTab();
								if (u.isScriptEdt)
									Ds.focusEdt(u.scriptEditor);
							}
						},
						beforedestroy : function(t) {
							destroyCmp(t.objectTree.getRootNode());
						},
						destroy : function(t) {
							t.items.each(function(c) {
								Wb.closeNav(backBtn, forwardBtn, c);
							});
							Wb.closeNav(backBtn, forwardBtn, t);
							t.objectTree.store.destroyStore();
							Ds.setSaveBtn();
							if (t.ellipsisTip) {
								Ext.destroy(t.ellipsisTip);
								t.ellipsisTip = null;
							}
						},
						beforeclose : function(t) {
							if (t.isModified) {
								tab.setActiveTab(t);
								Wb.choose(Wb.format(Str.saveConfirm, t.title
										.substring(1)), function(b) {
									if (b == 'yes') {
										Ds.saveModule(false, function() {
											t.close();
										});
									} else if (b == 'no') {
										t.isModified = false;
										t.close();
									}
								});
								return false;
							}
						}
					},
					items : [ {
						title : 'General',
						layout : 'border',
						iconCls : 'home_icon',
						listeners : {
							render : function(t) {
								page.generalTab = t;
							}
						},
						items : [
								{
									region : 'west',
									split : true,
									border : false,
									xtype : 'tabpanel',
									deferredRender : false,
									width : 460,
									listeners : {
										beforetabchange : function(t) {
											Ds.svPos();
											Ds.completeEdit();
										},
										tabchange : function(a, b, c) {
											Ds.rsPos();
										},
										render : function(t) {
											page.contentTab = t;
										}
									},
									items : [ {
										title : 'Properties',
										iconCls : 'property_icon',
										xtype : 'propertygrid',
										nameColumnWidth : 170,
										source : {},
										listeners : {
											render : function(t) {
												page.propertyGrid = t;
												Ds.dropUrl(t);
											},
											validateedit : function(a, b) {
												return blockModify(a, b, this);
											}
										}
									}, {
										title : 'Events',
										iconCls : 'execute_icon',
										xtype : 'propertygrid',
										nameColumnWidth : 170,
										source : {},
										listeners : {
											render : function(t) {
												page.eventGrid = t;
											},
											validateedit : function(a, b) {
												return blockModify(a, b, this);
											}
										}
									} ]
								},
								{
									store : {
										proxy : {
											type : 'ajax',
											url : 'main?xwl=13L88K98GZO2&id=' + id,
											extraParams : {
												path : Ds.getIdePath()
											},
											listeners : {
												exception : function(p, r) {
													Wb.except(r.responseText)
												}
											}
										},
										fields : Ds.objectTreeFields,
										root : {
											MODULE_ID : -1,
											children : []
										},
										listeners : {
											beforeload : function() {
												Wb.mask(page);
											},
											load : function(s, r, b, u) {
												Wb.unmask(page);
												if (!u) {
													page.close();
													return;
												}
												var t = page.objectTree, n = t
														.getRootNode().firstChild;
												t.view.select(n);
												if (n.isExpandable()) {
													n.expand();
													n
															.eachChild(function(
																	x) {
																if (x
																		.get('xwlMeta') == 'viewport'
																		&& x
																				.isExpandable()) {
																	x.expand();
																	x
																			.eachChild(function(
																					y) {
																				if (y
																						.isExpandable())
																					y
																							.expand();
																			});
																}
															});
												}
												if (callback)
													callback(t, page);
											}
										}
									},
									rootVisible : false,
									animate : false,
									region : 'center',
									iconCls : 'list_icon',
									title : 'Object TreeView',
									tools : [
											{
												type : 'expand',
												tooltip : 'Expand All',
												handler : function() {
													page.objectTree.selectObject
															.expand(true);
												}
											},
											{
												type : 'collapse',
												tooltip : 'Collapse All',
												handler : function() {
													page.objectTree.selectObject
															.collapse(true);
												}
											} ],
									xtype : 'treepanel',
									viewConfig : {
										plugins : {
											pluginId : 'ddPlug',
											ptype : 'treeviewdragdrop',
											ddGroup : 'controlList'
										},
										listeners : {
											beforedrop : function(node, data,
													om, dp) {
												var v = Wb.getTree(data.item), fromControl = v == Wb
														.get('controlTree'), dst = Wb
														.getNode(node), src = Wb
														.getNode(data.item);
												if (fromControl)
													data.copy = true;
												if (Ds.canDroped(src, dst,
														dp == 'append')) {
													this.beforeParent = src.parentNode;
													return true;
												} else
													return false;
											},
											drop : function(node, data, om, dp) {
												var n = Wb.getDropedNode(node,
														dp), v = Wb
														.getTree(data.item), fromControl = v == Wb
														.get('controlTree'), absP, no;
												if (!fromControl
														&& (n.parentNode != this.beforeParent)) {
													Ds.delPdObj(
															this.beforeParent,
															n.get('text'));
													absP = Ds
															.findTab(n.parentNode);
													if (absP) {
														no = absP.absPanel
																.add(Ds
																		.getRendProp(
																				n,
																				8,
																				8,
																				n
																						.get('text')));
														Ds.selectObj(no, false);
													}
												}
												Ds.setModified();
												if (fromControl) {
													Ds.renameNode(n, true);
													Ds.setDisplay(n);
													page.objectTree.view
															.select(n);
													absP = Ds
															.findTab(n.parentNode);
													if (absP) {
														no = absP.absPanel
																.add(Ds
																		.getRendProp(
																				n,
																				8,
																				8,
																				n
																						.get('text')));
														Ds.selectObj(no, false);
													}
												}
											}
										}
									},
									listeners : {
										render : function(t) {
											page.objectTree = t;
										},
										itemmousedown : function(v, r) {
											Ds.moduleSeled = false;
											v.focus();
											Wb.setDDText(v, r.get('text'));
										},
										selectionchange : function(v, s, o) {
											Ds.completeEdit();
											var n = s[0];
											page.objectTree.selectObject = n;
											loadData(n);
										}
									}
								} ]
					} ]
				});
		tab.setActiveTab(page);
		page.objectTree.store.load();
	},
	decodeExpress : function(json) {
		var exp = {}, s;
		for (s in json)
			exp[s] = Ext.decode(json[s]);
		return exp;
	},
	expPack : function(f) {
		var s = '&root=' + (f ? moduleTree.selectModule.get('MODULE_ID') : '-1');
		Wb.download('main?xwl=13O5703A1GTM' + s);
	},
	expModule : function() {
		var t = Ds.at();
		if (t && t.objectTree)
			Wb.download('main?xwl=13O3H9TRCSYH', {
				title : t.orgTitle,
				moduleId : t.id.substring(2)
			});
		else
			Wb.warning('Please open a module to export.');
	},
	impModule : function() {
		var t = Ds.at();
		if (t && t.objectTree) {
			impWin.impType = 0;
			impWin.moduleId = t.id.substring(2);
			impWin.setTitle('Import to ' + t.orgTitle)
			impWin.show();
		} else
			Wb.warning('Please open a module to import.');
	},
	impPack : function() {
		impWin.impType = 1;
		impWin.setTitle('Import and merge modules')
		impWin.show();
	},
	finishImp : function(value) {
		impWin.hide();
		var t;
		if (impWin.impType == 0) {
			t = Ds.at().objectTree;
			Wb.delModified(Ds.at());
			t.store.load( {
				node : t.getRootNode()
			});
		} else {
			t = value;
			if (!Wb.isEmpty(t))
				t = '<br>Some duplicated modules id has been replaced:<br>' + t + '<br>';
			Wb.confirm('Import successfully, reload the page?' + t, function() {
				Wd.wb_forceCls = true;
				location.reload();
			});
		}
	},
	doImp : function() {
		if (impWin.impType == 0) {
			form1.url = 'main?xwl=13O3FO5EYL83';
			Wb.upload(form1, {
				moduleId : impWin.moduleId
			});
		} else {
			form1.url = 'main?xwl=13O5703A1GTK';
			Wb.upload(form1);
		}
	},
	mdBeforeChg : function(a, b, c) {
		Ds.svPos(1);
		if (c && c.tabBar && c.tabBar.layout)
			c.saveScrollPos = c.tabBar.layout.overflowHandler
					.getScrollPosition();
		Ds.completeEdit();
		if (c && !c.saveUrl) {
			var o;
			o = c.getActiveTab();
			if (o && o.commitChange)
				o.commitChange();
		}
	},
	mdChg : function(a, b, c) {
		Ds.rsPos(1);
		if (b && b.saveScrollPos !== undefined)
			b.tabBar.layout.overflowHandler.scrollTo(b.saveScrollPos, false);
		Wb.recNav(b, c, backBtn);
		if (b.isSaving)
			Wb.mask(b, Str.saving);
	},
	completeEdit : function() {
		var d = Ds.at();
		if (d) {
			d = d.contentTab;
			if (d)
				d = d.getActiveTab();
			if (d)
				d.plugins[0].completeEdit();
		}
	},
	setModified : function() {
		Wb.setModified(Ds.at());
		Ds.setSaveBtn();
	},
	setSaveBtn : function() {
		var b1 = true, b2 = true;
		moduleTab.items.each(function(c) {
			if (c.isModified) {
				b2 = false;
				if (c == Ds.at()) {
					b1 = false;
					return false;
				}
			}
		});
		saveBtn.setDisabled(b1);
		saveAllBtn.setDisabled(b2);
	},
	getModifiedTitle : function() {
		var t = moduleTab, s = null;
		t.items.each(function(c) {
			if (c.isModified) {
				s = c.orgTitle;
				t.setActiveTab(c);
				return false;
			}
		});
		return s;
	},
	addIconItem : function(combo, index) {
		var s = combo.getValue(), d = combo.displayField, v = combo.valueField, st = combo.store, n, i;
		if (Ext.isEmpty(s))
			s = '';
		if (combo.isValid() && !Wb.findRecord(st, d || v, s)) {
			n = {
				field1 : s,
				field2 : s,
				field3 : 'item'
			};
			st.clearFilter();
			if (Ext.isDefined(index)) {
				i = index;
				if (i < 0)
					i = st.count() + i + 1;
				st.insert(i, n);
			} else
				st.add(n);
			combo.setValue(combo.getValue());
		}
	},
	monitorIconFocus : function(o) {
		o.allowChange = true;
		Ds.addIconItem(o);
		var r = Wb.findRecord(o.store, o.displayField, '');
		if (r)
			o.store.remove(r);
	},
	monitorBindFocus : function(o) {
		var r, t = Ds.at().objectTree, x;
		o.allowChange = true;
		o.store.removeAll();
		o.bindItems = [];
		if (o.bindType == 'owned')
			x = null;
		else if (o.bindType == 'dOwned')
			x = undefined;
		else
			x = o.bindType;
		Ds
				.scanBindObj(t.getRootNode(), o.store, o.bindItems, x,
						t.selectObject);
		o.store.clearFilter();
		o.store.sort('field1', 'ASC');
	},
	scanBindObj : function(c, store, items, type, x) {
		var ct, np, vp, v, z;
		if (type === null)
			z = 0;
		else if (type === undefined)
			z = 1;
		else
			z = 2;
		c.eachChild(function(n) {
			ct = n.get('xwlCategory');
			np = Wb.getNamePart(type);
			if (np == 'array') {
				vp = Wb.getValuePart(type);
				if (Wb.isEmpty(vp))
					vp = null;
				else
					vp = vp.split(',');
			} else
				vp = null;
			if ((Wb.isEmpty(vp) || n.firstChild
					&& Wb.indexOf(vp, n.firstChild.get('xwlMeta')) != -1)
					&& (Wb.isEmpty(np)
							|| Wb.indexOf(np, n.get('xwlMeta')) != -1 || (!Wb
							.isEmpty(ct) && Wb.isAcross(np.split(','), ct
							.split(','))))
					&& (z == 2 || z == 0 && n.isAncestor(x) || z == 1
							&& n.parentNode == x)) {
				v = n.get('text');
				items.push(v);
				store.add( {
					field1 : v
				});
			}
			Ds.scanBindObj(n, store, items, type, x);
		});
	},
	getIdePath : function() {
		var p;
		if (idepath !== '-') {
			p = Wb.getSelNode(moduleTree);
			if (p)
				return p.getPath('MODULE_ID', '\n');
		}
		return '';
	},
	edtBind : function(type, mul) {
		return new Ext.form.field.ComboBox( {
			store : [ '' ],
			queryMode : 'local',
			enableKeyEvents : true,
			typeAhead : true,
			bindType : type,
			isDsCombo : true,
			multiSelect : mul,
			enableKeyEvents : true,
			listeners : {
				change : Ds.monitorChange,
				focus : Ds.monitorBindFocus
			}
		});
	},
	edtIcon : function() {
		return new Ext.form.field.ComboBox(
				{
					store : Ds.iconList,
					queryMode : 'local',
					enableKeyEvents : true,
					typeAhead : true,
					isDsCombo : true,
					listConfig : {
						getInnerTpl : function() {
							return '<div style="background-image:url(webbuilder/images/{field3}.gif);" class="wb_combo_icon">{field1}</div>';
						}
					},
					listeners : {
						change : Ds.monitorChange,
						focus : Ds.monitorIconFocus
					}
				});
	},
	edtDate : function() {
		return new Ext.form.field.Date( {
			format : 'Y-m-d',
			isDsCombo : true,
			getErrors : function(v) {
				return [];
			},
			rawToValue : function(rawValue) {
				return this.parseDate(rawValue, 'Y-m-d') || rawValue || '';
			},
			getValue : function() {
				var me = this, val = me.rawToValue(me.processRawValue(me
						.getRawValue())), s, r;
				me.value = val;
				s = me.valueToRaw(val);
				r = Wb.optString(s).split('-');
				if (r.length == 3 && Ext.isNumeric(r[0]) && Ext.isNumeric(r[1])
						&& Ext.isNumeric(r[2]))
					return 'new Date(' + parseInt(r[0], 10) + ','
							+ (parseInt(r[1], 10) - 1) + ','
							+ parseInt(r[2], 10) + ')';
				else
					return s;
			},
			valueToRaw : function(value) {
				return this.formatDate(this.parseDate(value, 'Y-m-d'), 'Y-m-d')
						|| value || '';
			},
			enableKeyEvents : true,
			listeners : {
				change : Ds.monitorChange,
				focus : Ds.monitorFocus
			}
		});
	},
	edtColor : function() {
		return new Wb.ide.ColorField( {
			isDsCombo : true,
			enableKeyEvents : true,
			listeners : {
				change : Ds.monitorChange,
				focus : Ds.monitorFocus
			}
		});
	},
	edtUrl : function(b) {
		return new Ext.form.field.Trigger( {
			enableKeyEvents : true,
			onTriggerClick : b ? Ds.promptUrls : Ds.promptUrl,
			enableKeyEvents : true,
			isUrl : true,
			listeners : {
				change : Ds.monitorChange,
				focus : Ds.monitorFocus,
				render : Ds.populateDblClick
			},
			editable : true,
			triggerCls : 'ellipsis_icon'
		});
	},
	edtBool : function() {
		return Ds.edtEnum( [ 'true', 'false' ]);
	},
	edtJndi : function() {
		return Ds.edtEnum(Ds.jndiList);
	},
	edtEnum : function(list, mul) {
		var d;
		if (Ext.isString(list))
			d = list.split(',');
		else
			d = list;
		return new Ext.form.field.ComboBox( {
			store : d,
			queryMode : 'local',
			typeAhead : true,
			isDsCombo : true,
			multiSelect : mul,
			enableKeyEvents : true,
			listeners : {
				change : Ds.monitorChange,
				focus : Ds.monitorCbFocus
			}
		});
	},
	edtTrigger : function(t) {
		return new Ext.form.field.Trigger( {
			enableKeyEvents : true,
			type : t,
			onTriggerClick : function(o) {
				var m;
				if (o.isTriggerField)
					m = o;
				else
					m = this;
				Ds.addSE(m, m.type);
			},
			enableKeyEvents : true,
			listeners : {
				render : Ds.populateDblClick
			},
			editable : false,
			blockPost : true,
			fieldStyle : 'background-color:#D0D0D0;background-image:none',
			triggerCls : 'ellipsis_icon'
		});
	},
	edtText : function(type) {
		var o = {
			enableKeyEvents : true,
			hideTrigger : true,
			listeners : {
				change : Ds.monitorChange,
				focus : Ds.monitorFocus
			}
		};
		if (type == 'id') {
			o.allowBlank = false;
			o.validator = Ds.idValidator;
		}
		return new Ext.form.field.Trigger(o);
	},
	ar : function(v) {
		var p = Ds.at(), g = p && p.contentTab ? p.contentTab.getActiveTab()
				: null, r;
		if (g) {
			r = g.plugins[0].activeRecord;
			return r ? r.get(v ? 'value' : 'name') : null;
		}
		return null;
	},
	svPos : function(b) {
		var a = Ds.at();
		if (a && a.generalTab && a.getActiveTab() == a.generalTab) {
			if (!a.propertyGrid.hidden)
				a.svU1 = a.propertyGrid.view.el.dom.scrollTop;
			if (!a.eventGrid.hidden)
				a.svU2 = a.eventGrid.view.el.dom.scrollTop;
			if (b)
				a.svU3 = a.objectTree.view.el.dom.scrollTop;
		}
	},
	rsPos : function(b) {
		var a = Ds.at(), x;
		if (a && a.generalTab && a.getActiveTab() == a.generalTab) {
			if (a.svU1 !== undefined && !a.propertyGrid.hidden) {
				x = a.propertyGrid.view.el.dom;
				x.scrollTop = 0;
				x.scrollTop = a.svU1;
			}
			if (a.svU2 !== undefined && !a.eventGrid.hidden) {
				x = a.eventGrid.view.el.dom;
				x.scrollTop = 0;
				x.scrollTop = a.svU2;
			}
			if (b && a.svU3 !== undefined) {
				x = a.objectTree.view.el.dom;
				x.scrollTop = 0;
				x.scrollTop = a.svU3;
			}
		}
	},
	ajustAbsPos : function(l, shift) {
		var p = Ds.pn(), x = 0, o, u, z;
		p.items.each(function(c) {
			if (c.ideSel) {
				o = c;
				u = Ext.fly(c.id + '-rzwrap');
				z = Ds.gxy(c);
				x++;
				switch (l) {
				case 37:
					if (shift) {
						u.setWidth(c.getWidth() - 1);
						c.setWidth(c.getWidth() - 1);
					} else
						u.setLeft(z[0] - 1);
					break;
				case 38:
					if (shift) {
						u.setHeight(c.getHeight() - 1);
						c.setHeight(c.getHeight() - 1);
					} else
						u.setTop(z[1] - 1);
					break;
				case 39:
					if (shift) {
						u.setWidth(c.getWidth() + 1);
						c.setWidth(c.getWidth() + 1);
					} else
						u.setLeft(z[0] + 1);
					break;
				case 40:
					if (shift) {
						u.setHeight(c.getHeight() + 1);
						c.setHeight(c.getHeight() + 1);
					} else
						u.setTop(z[1] + 1);
					break;
				}
			}
		});
		if (x == 1)
			Ds.updateAbsInfo(o);
		if (x > 0)
			Ds.setModified();
	},
	selAllItems : function() {
		Ds.selectAll(Ds.pn(), true);
	},
	monitorKey : function(e, o) {
		Ds.ctrlKey = e.ctrlKey;
		if (e.ctrlKey) {
			if (Ds.urlWin && Ds.urlWin.isVisible() || Ds.urlsWin
					&& Ds.urlsWin.isVisible())
				return;
			var t = e.getTarget(), keyCode = e.getKey(), k = String
					.fromCharCode(keyCode), at = Ds.at(), ats = at
					&& !at.saveUrl ? at.getActiveTab() : null, fp;
			if (at && at.saveUrl)
				return;
			Ds.keyCode = keyCode;
			if ((k == 'C' || k == 'V' || k == 'X' || k == 'A' || keyCode > 36
					&& keyCode < 41)
					&& (Wb.isEditor(t) || ats && ats.scriptEditor
							&& !Ds.slModule()))
				return;
			if (keyCode == 46) {
				if (e.shiftKey)
					Ds.removeChildren();
				else
					Ds.removeControl();
				return;
			}
			fp = ats && ats.absPanel;
			if (keyCode > 36 && keyCode < 41 && fp) {
				e.stopEvent();
				Ds.ajustAbsPos(keyCode, e.shiftKey);
				return;
			}
			switch (k) {
			case 'A':
				if (fp) {
					e.stopEvent();
					Ds.selectAll(fp, true);
				}
				break;
			case 'X':
				e.stopEvent();
				Ds.cutControl();
				break;
			case 'Q':
				e.stopEvent();
				Ds.delayRun();
				break;
			case 'J':
				e.stopEvent();
				Ds.showGeneral();
				break;
			case 'K':
				e.stopEvent();
				searchBtn.fireEvent('click');
				break;
			case 'C':
				e.stopEvent();
				Ds.copyControl();
				break;
			case 'E':
				e.stopEvent();
				Ds.getModuleProperties();
				break;
			case 'N':
				e.stopEvent();
				Ds.createModule(e.shiftKey);
				break;
			case 'M':
				e.stopEvent();
				Ds.createFolder(e.shiftKey);
				break;
			case 'D':
				if (ats && ats.scriptEditor)
					return;
				e.stopEvent();
				Ds.editAbsLayout();
				break;
			case 'V':
				e.stopEvent();
				Ds.pasteControl();
				break;
			case 'S':
				e.stopEvent();
				if (e.shiftKey)
					Ds.delaySave(true);
				else
					Ds.delaySave(false);
				break;
			}
		}
	},
	monitorChange : function(o) {
		if (o.allowChange && o.isValid())
			Ds.setProperty(o);
	},
	monitorFocus : function(o) {
		o.allowChange = true;
	},
	monitorCbFocus : function(o) {
		o.allowChange = true;
	},
	promptUrl : function(o) {
		Ds.promptResource.call(this, o, false);
	},
	promptUrls : function(o) {
		Ds.promptResource.call(this, o, true);
	},
	promptResource : function(o, list) {
		var v, n, page = Ds.at(), pg = page.propertyGrid, x, tls, xn = Ds.ar();
		Ds.completeEdit();
		tls = [ {
			tooltip : Str.refresh,
			handler : function() {
				if (Wb.get('urlSelPanel' + list).getActiveTab() == Wb
						.get('urlModPanel' + list))
					Wb.refresh(Wb.get('urlTree' + list), 'MODULE_ID');
				else
					Wb.refresh(Wb.get('resourceTree' + list), 'text');
			},
			type : 'refresh'
		} ];
		if (list) {
			x = Ds.urlsWin;
			tls.push( {
				tooltip : 'Uncheck All',
				type : 'minus',
				handler : function() {
					var t;
					if (Wb.get('urlSelPanel' + list).getActiveTab() == Wb
							.get('urlModPanel' + list))
						t = 'urlTree';
					else
						t = 'resourceTree';
					Wb.check(Wb.get(t + list), false, false, true);
				}
			});
		} else
			x = Ds.urlWin;
		if (x) {
			x.thePg = pg;
			x.theXn = xn;
			x.show();
		} else
			new Ext.window.Window(
					{
						title : 'Resource Selector',
						width : 400,
						height : 430,
						layout : 'fit',
						closeAction : 'hide',
						iconCls : 'web_icon',
						tools : tls,
						okHandler : function() {
							var ut = Wb.get('urlTree' + list), rt = Wb
									.get('resourceTree' + list), ck, t;
							if (Wb.get('urlSelPanel' + list).getActiveTab() == Wb
									.get('urlModPanel' + list)) {
								if (list) {
									ck = ut.getChecked();
									v = [];
									Ext.Array.each(ck, function(n) {
										v.push('#' + n.get('MODULE_ID') + ' ('
												+ n.get('orgText') + ')');
									});
									v = Ext.encode(v);
								} else {
									n = Wb.getSelNode(ut);
									if (n.get('IS_FOLDER'))
										return;
									v = '#' + n.get('MODULE_ID') + ' ('
											+ n.get('orgText') + ')';
								}
							} else {
								if (list) {
									ck = rt.getChecked();
									v = [];
									Ext.Array.each(ck, function(n) {
										t = n.getPath('text', '/');
										t = t.substring(t.indexOf('/', 1) + 1);
										v.push(t);
									});
									v = Ext.encode(v);
								} else {
									n = Wb.getSelNode(rt);
									if (n.get('isDir'))
										return;
									v = n.getPath('text', '/');
									v = v.substring(v.indexOf('/', 1) + 1);
								}
							}
							x.thePg.setProperty(x.theXn, v);
							Ds.setModified();
							x.hide();
						},
						buttons : [ {
							text : Str.ok,
							iconCls : 'ok_icon',
							handler : Wb.okHandle
						}, {
							text : Str.cancel,
							iconCls : 'cancel_icon',
							handler : Wb.cancelHandle
						} ],
						modal : true,
						resizable : false,
						listeners : {
							render : function(o) {
								if (list)
									Ds.urlsWin = o;
								else
									Ds.urlWin = o;
								o.thePg = pg;
								o.theXn = xn;
								x = o;
								Wb.monEnter(o);
							},
							show : function() {
								if (list) {
									Wb.check(Wb.get('urlTree' + list), false,
											false, true);
									Wb.check(Wb.get('resourceTree' + list),
											false, false, true);
								}
							}
						},
						items : [ {
							xtype : 'tabpanel',
							id : 'urlSelPanel' + list,
							items : [
									{
										title : 'Modules',
										id : 'urlModPanel' + list,
										layout : 'fit',
										iconCls : 'module_icon',
										xtype : 'panel',
										items : [ {
											xtype : 'treepanel',
											id : 'urlTree' + list,
											store : {
												proxy : {
													type : 'ajax',
													url : 'main?xwl=13L88K98GZO3&check=' + list,
													listeners : {
														exception : function(p,
																r) {
															Wb
																	.except(r.responseText)
														}
													}
												},
												fields : Ds.moduleTreeFields,
												listeners : {
													beforeload : function(a, b) {
														b.params.parentId = b.node
																.get('MODULE_ID');
													}
												},
												sorters : [ {
													property : 'ORDER_INDEX',
													direction : 'ASC'
												} ]
											},
											rootVisible : false,
											animate : false,
											listeners : {
												itemdblclick : function(o) {
													if (!list)
														x.okHandler();
												}
											}
										} ]
									},
									{
										title : 'Server Files',
										id : 'urlResPanel' + list,
										layout : 'fit',
										iconCls : 'resource_icon',
										xtype : 'panel',
										items : [ {
											xtype : 'treepanel',
											id : 'resourceTree' + list,
											store : {
												proxy : {
													type : 'ajax',
													url : 'main?xwl=13M5W3GMF7ZS&check=' + list,
													listeners : {
														exception : function(p,
																r) {
															Wb
																	.except(r.responseText)
														}
													}
												},
												fields : [ {
													name : 'text'
												}, {
													name : 'dir'
												}, {
													name : 'isDir'
												} ],
												listeners : {
													beforeload : function(a, b) {
														var d;
														if (Wb.isEmpty(b.node
																.get('dir')))
															d = '@';
														else
															d = b.node
																	.get('dir');
														b.params.dir = d;
													}
												}
											},
											rootVisible : false,
											animate : false,
											listeners : {
												itemdblclick : function(o) {
													if (!list)
														x.okHandler();
												}
											}
										} ]
									} ]
						} ]
					}).show();
	},
	populateDblClick : function(o) {
		o.isTriggerField = true;
		o.inputEl.dom.ondblclick = function() {
			o.onTriggerClick(o);
		}
	},
	close : function(f) {
		var o, t = moduleTab;
		if (f < 3) {
			o = t.getActiveTab();
			t.items.each(function(p) {
				if (f == 1 || o != p) {
					if (p.isModified) {
						t.setActiveTab(p);
						Wb.choose(Wb.format(Str.saveConfirm, p.title
								.substring(1)), function(b) {
							if (b == 'yes') {
								Ds.saveModule(false, function() {
									Ds.close(f);
								});
							} else if (b == 'no') {
								p.isModified = false;
								Ds.close(f);
							}
						});
						return false;
					} else
						p.close();
				}
			});
		} else {
			t = t.getActiveTab();
			if (t && t.generalTab) {
				o = t.getActiveTab();
				if (f == 3)
					t.setActiveTab(t.generalTab);
				t.items.each(function(p) {
					if (p != t.generalTab && (f == 3 || o != p))
						p.close();
				});
			}
		}
	},
	addSE : function(obj, type) {
		function setScript(edit) {
			var p = selObj.get('properties')[propName] != null, v = edit
					.getValue();

			if (p) {
				if (pg.source == selObj.get('properties'))
					pg.setProperty(propName, v);
				else
					selObj.get('properties')[propName] = v;
			} else {
				if (eg.source == selObj.get('events'))
					eg.setProperty(propName, v);
				else
					selObj.get('events')[propName] = v;
			}
		}

		var page = Ds.at(), pg = page.propertyGrid, eg = page.eventGrid, propName = Ds
				.ar(), val = Ds.ar(true), selObj = page.objectTree.selectObject, objName = selObj
				.get('text'), scriptEditor, ex = false, scriptTabObj, pHint, edtId;
		Ds.completeEdit();
		page.items.each(function(x) {
			if (x.bindObj === selObj && x.bdPropName === propName) {
				page.setActiveTab(x);
				ex = true;
				return false;
			}
		});
		if (ex)
			return;
		pHint = selObj.get('custEPara')[propName];
		if (Ext.isEmpty(pHint))
			pHint = '';
		else
			pHint = 'function(' + pHint + ')';
		edtId = Ds.getId();
		page.add( {
			iconCls : type + '_icon',
			title : objName + '.' + propName,
			closable : true,
			isScriptEdt : true,
			bindObj : selObj,
			bdPropName : propName,
			layout : 'fit',
			bbar : [
					type == 'js' || type == 'ss' ? {
						iconCls : 'ok_icon',
						tooltip : 'Verify Script',
						handler : function() {
							try {
								eval('function sys_wb_verify(){' + scriptEditor
										.getValue() + '}');
								Wb.message('Verify Successful.');
							} catch (e) {
								Wb.error(e.message);
							}
						}
					} : {
						iconCls : 'null_icon',
						disabled : true
					}, ' ', {
						text : '1 : 1',
						xtype : 'tbtext',
						listeners : {
							render : function(t) {
								scriptTabObj.cursorLabel = t;
							}
						}
					}, '->', {
						text : pHint,
						xtype : 'tbtext'
					} ],
			listeners : {
				render : function(t) {
					scriptTabObj = t;
					t.commitChange = function() {
						if (t.isModified) {
							setScript(scriptEditor);
							t.isModified = false;
						}
					}
					scriptEditor = CodeMirror.fromTextArea(Wb.dom(edtId), {
						lineNumbers : true,
						mode : type == 'sql' ? 'text/x-plsql' : (type == 'js'
								|| type == 'ss' ? 'text/javascript'
								: 'text/html'),
						onChange : function() {
							if (scriptEditor.sttChg) {
								scriptTabObj.isModified = true;
								Ds.setModified();
							}
						},
						onCursorActivity : function() {
							var o = scriptEditor.getCursor();
							scriptTabObj.cursorLabel.setText((o.line + 1) + ':'
									+ (o.ch + 1));
						}
					});
					t.scriptEditor = scriptEditor;
				},
				destroy : function(t) {
					Wb.closeNav(backBtn, forwardBtn, t);
				},
				resize : function(a, b, c) {
					Ext.fly(scriptEditor.getScrollerElement())
							.setHeight(c - 32);
					scriptEditor.refresh();
				},
				activate : function(t) {
					Ds.focusEdt(scriptEditor);
				}
			},
			items : [ {
				xtype : 'panel',
				html : '<div id="' + edtId + '"></div>'
			} ]
		});
		page.setActiveTab(scriptTabObj);
		scriptEditor.setValue(val);
		scriptEditor.sttChg = 1;
	},
	focusEdt : function(o) {
		setTimeout(function() {
			o.focus();
		}, 20);
	},
	getNodeData : function(data, node) {
		data['xwlMeta'] = node.get('xwlMeta');
		var e, p = node.get('properties'), k, rl;
		for (k in p) {
			if (!Ext.isEmpty(p[k]))
				data[k] = p[k];
		}
		e = node.get('events');
		for (k in e) {
			if (!Ext.isEmpty(e[k]))
				data[k] = e[k];
		}
		if (node.hasChildNodes()) {
			data['children'] = [];
			node.eachChild(function(n) {
				rl = {};
				Ds.getNodeData(rl, n);
				data['children'].push(rl);
			});
		}
	},
	delaySave : function(isAll, func) {
		if (!Ds.delaySv)
			Ds.delaySv = new Ext.util.DelayedTask(Ds.saveModule);
		Ds.delaySv.delay(102, null, null, [ isAll, func ]);
	},
	saveModule : function(isAll, func) {
		var d, o, para = {}, tab = moduleTab, at = tab.getActiveTab(), tbs = [];
		if (at == null || at.saveUrl)
			return;
		o = at.getActiveTab();
		if (o.commitChange)
			o.commitChange();
		tab.items.each(function(t) {
			if (t.isModified && !t.isSaving && (isAll || t == at)) {
				d = {};
				tbs.push(t);
				t.isSaving = true;
				Wb.mask(t, Str.saving);
				Ds.getNodeData(d, t.objectTree.getRootNode().firstChild);
				para['xwl_' + t.id.substring(2)] = Ext.encode(d);
			}
		});
		if (tbs.length > 0) {
			Wb.request( {
				url : 'main?xwl=13L88K98GZO4',
				params : para,
				showMask : false,
				callback : function() {
					Ext.Array.each(tbs, function(t) {
						t.isSaving = false;
						Wb.unmask(t);
					});
				},
				success : function(r) {
					tab.items.each(function(t) {
						if (t.isModified && (isAll || t == at))
							Wb.delModified(t);
					});
					Ds.setSaveBtn();
					if (func)
						func();
				},
				failure : function(r) {
					Wb.except(r.responseText);
				}
			});
		}
	},
	createModule : function(isInsert) {
		Ds.isFolder = false;
		Ds.isInsert = isInsert;
		var w = newWin;
		w.show();
		Ds.setWinProp(false);
		w.setTitle((isInsert ? 'Insert' : 'New') + ' Module');
		w.setIconCls('new_icon');
		newWinCheck.setDisabled(false);
		w.center();
	},
	createFolder : function(isInsert) {
		Ds.isFolder = true;
		Ds.isInsert = isInsert;
		var w = newWin;
		w.show();
		Ds.setWinProp(false);
		w.setTitle((isInsert ? 'Insert' : 'New') + ' Folder');
		w.setIconCls('folder_icon');
		newWinCheck.setDisabled(true);
		w.center();
	},
	finalize : function() {
		Ext.getDoc().on('keydown', Ds.monitorKey);
	},
	cloneNodeProperty : function(node) {
		var s, n, p = {}, e = {};

		n = node.get('properties');
		for (s in n)
			p[s] = n[s];
		n = node.get('events');
		for (s in n)
			e[s] = n[s];
		node.set('properties', p);
		node.set('events', e);
		node.commit();
	},
	setWinProp : function(isProp) {
		var w = newWin;
		if (isProp) {
			w.setHeight(265);
			hiddenCheck.el.setTop(172);
			newWinCheck.el.setTop(172);
		} else {
			w.setHeight(170);
			hiddenCheck.el.setTop(77);
			newWinCheck.el.setTop(77);
		}
		createDate.setVisible(isProp);
		lastModiDate.setVisible(isProp);
		createLabel.setVisible(isProp);
		lastModiLabel.setVisible(isProp);
		idLabel.setVisible(isProp);
		moduleIdEdit.setVisible(isProp);
	},
	locateItem : function(record) {
		var r = record, s, n, p, f;
		Ds.createTab(r.get('module'), r.get('moduleId'), r.get('iconCls'),
				function(t, tab) {
					tab.setActiveTab(tab.generalTab);
					s = r.get('name');
					p = s.indexOf('.');
					n = t.getRootNode().findChild('text', s.substring(0, p),
							true);
					if (n) {
						t.selectPath(n.getPath('text'), 'text');
						t.view.select(n);
						s = s.substring(p + 1);
						f = Wb.findRecord(tab.propertyGrid.store, 'name', s);
						if (!Ds.delayFcp)
							Ds.delayFcp = new Ext.util.DelayedTask(function(a,
									b) {
								a.startEdit(b, 1);
							});
						if (f) {
							tab.contentTab.setActiveTab(tab.propertyGrid);
							Ds.delayFcp.delay(50, null, null, [
									tab.propertyGrid.plugins[0], f ]);
						} else {
							f = Wb.findRecord(tab.eventGrid.store, 'name', s);
							if (f) {
								tab.contentTab.setActiveTab(tab.eventGrid);
								Ds.delayFcp.delay(50, null, null, [
										tab.eventGrid.plugins[0], f ]);
							}
						}
					}
				});
	},
	getModuleProperties : function() {
		var m = moduleTree.selectModule, w, f;
		if (m == null) {
			Wb.warning('Please select a module.');
			return;
		}
		Ds.isFolder = 'property';
		w = newWin;
		f = m.get('IS_FOLDER');
		w.show();
		Ds.setWinProp(true);
		titleEdit.setValue(m.get('orgText'));
		iconCombo.setValue(m.get('iconCls'));
		moduleIdEdit.setValue(m.get('MODULE_ID'));
		hiddenCheck.setValue(m.get('IS_HIDDEN'));
		newWinCheck.setValue(m.get('NEW_WIN'));
		newWinCheck.setDisabled(f);
		createDate.setValue(Wb.dateToStr(m.get('CREATE_DATE')));
		lastModiDate.setValue(Wb.dateToStr(m.get('LAST_MODIFY_DATE')));
		if (f)
			w.setTitle('Folder Properties');
		else
			w.setTitle('Module Properties');
		w.setIconCls('property_icon');
		w.center();
	},
	setModule : function() {
		var t = moduleTree, m = t.selectModule, id = m.get('MODULE_ID'), title = titleEdit
				.getValue(), icon = iconCombo.getValue(), hidden = hiddenCheck
				.getValue() ? 1 : 0, nw = newWinCheck.getValue() ? 1 : 0;

		Wb
				.request( {
					url : 'main?xwl=13L88K98GZOF',
					message : Str.saving,
					params : {
						title : title,
						icon : icon,
						newWin : nw,
						hidden : hidden,
						id : id
					},
					success : function(resp) {
						var r = Ext.decode(resp.responseText), o, et, isHidden = hidden == 1, isNewWin = nw == 1;
						if (isHidden)
							m.set('cls', 'wb_blue');
						else
							m.set('cls', '');
						m.set('text', r.title);
						m.set('orgText', title);
						m.set('iconCls', icon);
						m.set('IS_HIDDEN', isHidden);
						m.set('NEW_WIN', isNewWin);
						m.set('LAST_MODIFY_USER', r.user);
						m.set('LAST_MODIFY_DATE', r.date);
						m.commit();
						o = Wb.get('t_' + id);
						if (o) {
							et = Wb.ellipsis(r.title);
							o.setIconCls(icon);
							o.setTitle((o.isModified ? '*' : '') + et);
							o.orgTitle = r.title;
							if (et !== r.title) {
								if (o.ellipsisTip)
									o.ellipsisTip.update(r.title);
								else {
									o.ellipsisTip = new Ext.tip.ToolTip( {
										target : o.tab.btnWrap,
										html : r.title
									});
								}
							} else if (o.ellipsisTip) {
								Ext.destroy(o.ellipsisTip);
								o.ellipsisTip = null;
							}
						}
						newWin.hide();
					}
				});
	},
	newModule : function() {
		var t = moduleTree, m = t.selectModule, pn, p = m ? m.parentNode : null;
		if (m) {
			if (m.get('IS_FOLDER'))
				pn = m;
			else
				pn = p;
		} else
			pn = t.getRootNode();
		pn
				.expand(
						false,
						function() {
							var id, index, dp, pn, title = titleEdit.getValue(), icon = iconCombo
									.getValue(), hidden = hiddenCheck
									.getValue() ? 1 : 0, nw = newWinCheck
									.getValue() ? 1 : 0;
							if (m) {
								dp = m.getDepth();
								if (Ds.isInsert) {
									if (dp == 1)
										id = '-1';
									else
										id = p.get('MODULE_ID');
									pn = p;
									index = m.get('ORDER_INDEX');
								} else {
									if (m.get('IS_FOLDER')) {
										id = m.get('MODULE_ID');
										index = m.lastChild ? m.lastChild
												.get('ORDER_INDEX') + 1 : 1;
										pn = m;
									} else {
										if (dp == 1)
											id = '-1';
										else
											id = p.get('MODULE_ID');
										pn = p;
										index = p.lastChild ? p.lastChild
												.get('ORDER_INDEX') + 1 : 1;
									}
								}
							} else {
								id = '-1';
								pn = t.getRootNode();
								if (Ds.isInsert)
									index = 1;
								else
									index = pn.lastChild ? pn.lastChild
											.get('ORDER_INDEX') + 1 : 1;
							}
							Wb
									.request( {
										url : 'main?xwl=13L88K98GZOC',
										message : Str.creating,
										params : {
											title : title,
											icon : icon,
											hidden : hidden,
											newWin : nw,
											parentId : id,
											orderIndex : index,
											content : Ds.isFolder ? '' : Ext
													.encode( {
														xwlMeta : 'module',
														id : 'module',
														title : title
													})
										},
										success : function(resp) {
											var n, y, ret = Ext
													.decode(resp.responseText), r = ret.info, d;
											pn
													.eachChild(function(c) {
														y = c
																.get('ORDER_INDEX');
														if (y >= index) {
															c
																	.set(
																			'ORDER_INDEX',
																			y + 1);
															c.commit();
														}
													});
											d = {
												text : r.title,
												orgText : title,
												iconCls : icon,
												MODULE_ID : r.id,
												PARENT_ID : id,
												CREATE_USER : r.user,
												CREATE_DATE : r.date,
												LAST_MODIFY_DATE : r.date,
												IS_HIDDEN : hidden == 1,
												NEW_WIN : nw == 1,
												IS_FOLDER : Ds.isFolder,
												ORDER_INDEX : index,
												leaf : !Ds.isFolder,
												children : []
											};
											if (hidden)
												d.cls = 'wb_blue';
											newWin.hide();
											if (Ds.isInsert) {
												if (m)
													n = pn.insertBefore(d, m);
												else {
													if (t.getRootNode.firstChild)
														n = pn
																.insertBefore(
																		d,
																		t.getRootNode.firstChild);
													else
														n = pn.appendChild(d);
												}
											} else
												n = pn.appendChild(d);
											t.view.select(n);
											if (!Ds.isFolder)
												Ds.createTab(r.title, r.id,
														icon);
										}
									});
						});
	},
	saveModuleTree : function(node, data, om, dp) {
		var tree = moduleTree, dst = Wb.getNode(node), src = Wb
				.getNode(data.item), orderIndex, srcId, parentId, pn;

		Wb.mask(tree);
		dst
				.expand(
						false,
						function() {
							Wb.unmask(tree);
							srcId = src.get('MODULE_ID');
							if (dp == 'append') {
								pn = dst;
								parentId = dst.get('MODULE_ID');
								orderIndex = dst.lastChild.previousSibling ? dst.lastChild.previousSibling
										.get('ORDER_INDEX') + 1
										: 1;
							} else {
								pn = src.parentNode;
								if (src.getDepth() == 1)
									parentId = '-1';
								else
									parentId = pn.get('MODULE_ID');
								if (dp == 'before')
									orderIndex = dst.get('ORDER_INDEX');
								else
									orderIndex = dst.get('ORDER_INDEX') + 1;
							}
							Wb.request( {
								url : 'main?xwl=13L88K98GZOE',
								mask : tree,
								params : {
									orderIndex : orderIndex,
									srcId : srcId,
									parentId : parentId
								},
								failure : function() {
									Wb.revertNodePos(src);
								},
								success : function() {
									pn.eachChild(function(x) {
										var i = x.get('ORDER_INDEX');
										if (i >= orderIndex) {
											x.set('ORDER_INDEX', i + 1);
											x.commit();
										}
									});
									src.set('PARENT_ID', parentId);
									src.set('ORDER_INDEX', orderIndex);
									src.commit();
								}
							});
						});
	},
	delModule : function(isAll) {
		var t = moduleTree, n = t.selectModule, ht;
		if (!n) {
			Wb.warning('Please select module you want to delete.');
			return;
		}
		if (isAll)
			ht = 'Are you sure you want to delete "{0}"?';
		else
			ht = 'Are you sure you want to delete children of "{0}"?';
		Wb.confirm(Wb.format(ht, n.get('text')), function() {
			var id = n.get('MODULE_ID');
			Wb.request( {
				url : 'main?xwl=13L88K98GZOD',
				mask : t,
				params : {
					id : id,
					isAll : isAll
				},
				success : function(resp) {
					if (isAll)
						Wb.delSelNode(t);
					else
						n.removeAll();
					var r = resp.responseText.split(','), i, j = r.length, b;
					for (i = 0; i < j; i++) {
						b = Wb.get('t_' + r[i]);
						if (b) {
							b.isModified = false;
							b.close();
						}
					}
				}
			});
		}, t.view.getNode(n));
	},
	refresh : function() {
		Wb.confirm('Are you sure you want to refresh the system?', function() {
			Wb.request( {
				url : 'main?xwl=13O3FEKV30SJ',
				success : function() {
					Wb.message('The system has been refreshed.');
				}
			});
		});
	},
	changeId : function(node, value) {
		var t, o;
		node.set('text', value);
		node.commit();
		Ds.at().items.each(function(c) {
			if (c.bindObj == node) {
				if (c.bdPropName)
					c.setTitle(value + '.' + c.bdPropName);
				else {
					c.setTitle(value);
					c.xwlId = value;
				}
			}
		});
		t = Ds.findTab(node.parentNode);
		if (t) {
			t = t.absPanel;
			if (t) {
				o = Ds.findObj(t, node);
				if (o.xwlId)
					o.xwlId = value;
				else
					o.setValue(value);
			}
		}
	},
	idValidator : function(v) {
		if (!this.ownerCt.isVisible())
			return true;
		var t = Ds.at().objectTree, r = t.getRootNode(), s = t.selectObject, uv = v
				.toUpperCase(), n = r.findChildBy(function() {
			return this.get('text').toUpperCase() == uv;
		}, null, true);
		if (Wb.verifyName(v) !== true)
			return Wb.format(Str.invalidName, v);
		if (n != null && n != s)
			return Wb.format(Str.alreadyExists, v);
		return true;
	},
	idValidatorBd : function(v) {
		var t = Ds.at().objectTree, r = t.getRootNode(), uv = v.toUpperCase(), n = r
				.findChildBy(function() {
					return this.get('text').toUpperCase() == uv;
				}, null, true);
		if (Wb.verifyName(v) !== true)
			return Wb.format(Str.invalidName, v);
		if (n != null && n != this.bindNode)
			return Wb.format(Str.alreadyExists, v);
		return true;
	},
	setProperty : function(obj) {
		var p = Ds.at(), g = p.contentTab.getActiveTab(), a = Ds.ar(), v = obj
				.getValue();

		if (v == null)
			v = '';
		if (a != null) {
			if (g.source[a] === v)
				return;
			g.source[a] = v;
			if (a == 'id')
				Ds.changeId(p.objectTree.selectObject, v)
		}
		Ds.setModified();
	},
	canDroped : function(src, tgt, isAppend) {
		if (src == null || tgt == null)
			return false;
		function accepted(t, m, c) {
			var tl = t.split(','), cl;
			if (c)
				cl = c.split(',');
			else
				cl = [];
			cl.push(m);
			return Wb.isAcross(tl, cl);
		}

		var dst, p, s;
		if (tgt.getDepth() == 1 && !isAppend)
			return false;
		if (isAppend)
			dst = tgt;
		else
			dst = tgt.parentNode;
		p = src.get('xwlParent');
		s = dst.get('xwlChildren');
		return (s == '*' || s != null
				&& accepted(s, src.get('xwlMeta'), src.get('xwlCategory')))
				&& (p == '*' || p != null
						&& accepted(p, dst.get('xwlMeta'), dst
								.get('xwlCategory')));
	},
	renameNode : function(node, addIndex) {
		Ds.cloneNodeProperty(node);
		var r = Wb.getRootNode(node), i = 1, t, v = node.get('properties').id;

		if (addIndex) {
			do {
				t = v + (i++);
			} while (r.findChild('text', t, true));
		} else {
			t = v;
			node.set('text', '.');
			while (r.findChild('text', t, true)) {
				t = v + (i++);
			}
		}
		node.get('properties').id = t;
		node.set('text', t);
		node.commit();
	},
	getSelNode : function() {
		var page = Ds.at();

		if (page == null)
			return;
		return page.objectTree.selectObject;

	},
	getSnap : function(p) {
		var m = p % 8;
		p = p - m;
		if (m > 4)
			return p + 8;
		return p;
	},
	ssz : function(c, x, y, w, h) {
		var o = Ext.fly(c.id + '-rzwrap');
		o.setLeftTop(x, y);
		o.setWidth(w);
		o.setHeight(h);
		c.setWidth(w);
		c.setHeight(h);
	},
	dragHandle : function(o) {
		var p = o.lastXY, fx = Ds.getSnap(p[0] - o.sfx), fy = Ds.getSnap(p[1]
				- o.sfy), ct = 0, obj;
		Ds.pn().items.each(function(c) {
			if (c.ideSel) {
				ct++;
				obj = c;
				Ext.fly(c.id + '-rzwrap')
						.setLeftTop(c.orgFx + fx, c.orgFy + fy);
			}
		});
		if (ct == 1)
			Ds.updateAbsInfo(obj);
		Ds.setModified();
	},
	gxy : function(o) {
		var p = Ds.pn().el.getXY(), c = o.el.getXY();
		return [ c[0] - p[0] - 1, c[1] - p[1] - 1 ];
	},
	dragMousedown : function(o, e) {
		if (e.target.clientWidth == 5 && e.target.clientHeight == 5)
			return false;
		var p = o.lastXY, x = Ds.pn(), u = x.el.getXY(), z;
		o.sfx = p[0];
		o.sfy = p[1];
		x.items.each(function(c) {
			z = c.el.getXY();
			c.orgFx = z[0] - u[0] - 1;
			c.orgFy = z[1] - u[1] - 1;
		});
	},
	restore : function() {
		Wb
				.confirm(
						'Are you sure you want to restore?',
						function() {
							Wb
									.request( {
										url : 'main?xwl=13NU7H8CPISE',
										timeout : -1,
										output : findBk,
										failureConfirm : function() {
											findBk.focus(false, true);
										},
										success : function() {
											restoreWin.hide();
											Wb
													.message(
															'Restore successfully. Please click OK to reload the page.',
															function() {
																Wd.wb_forceCls = true;
																location
																		.reload();
															});
										}
									});
						});
	},
	backup : function() {
		Wb.prompt('Backup', [ {
			text : 'Title',
			value : 'Backup',
			allowBlank : false
		} ], function(v) {
			Wb.request( {
				url : 'main?xwl=13NU7H8CPISB',
				timeout : -1,
				params : {
					title : v[0]
				},
				failureConfirm : function() {
					Wb.promptEditors[0].focus(false, true);
				},
				success : function() {
					Wb.closePrompt();
					Wb.message('Backup successfully.');
				}
			});
		}, false);
	},
	propEventHandle : {
		change : function(o) {
			if (Ds.stopRecChg)
				return;
			if (!o.isValid())
				return;
			var t = Ds.at().getActiveTab();
			if (!t.absPanel)
				return;
			Ds.stopSetProp = true;
			if (o.xwlId)
				t.textEdit.setValue(o.getValue());
			else {
				Ds.changeId(o.bindNode, o.getValue());
				if (t.idEdit.getValue().substring(0, 1) != '(')
					t.idEdit.setValue(o.getValue());
			}
			Ds.setModified();
			Ds.stopSetProp = false;
		},
		render : function(o) {
			var b = false;
			if (o.inputEl) {
				o.inputEl.dom.ondblclick = function() {
					if (Ext.EventObject.ctrlKey) {
						b = true;
						o.focus(true);
					}
				};
				o.inputEl.dom.onblur = function() {
					b = false;
					if (!o.isValid()) {
						Ds.stopRecChg = true;
						o.setValue(o.bindNode.get('text'));
						Ds.stopRecChg = false;
					}
				};
				o.inputEl.dom.onfocus = function() {
					if (!b)
						this.blur();
				};
			}
		}
	},
	showGeneral : function() {
		var t = Ds.at();
		if (t && t.generalTab)
			t.setActiveTab(t.generalTab);
	},
	getRendProp : function(node, sx, sy, newId) {
		var r = {}, d = node.get('properties'), id = d.id, nw, nh, xtype = node
				.get('xwlXtype'), category = node.get('xwlCategory'), minW = node
				.get('xwlMinWidth'), maxW = node.get('xwlMaxWidth'), minH = node
				.get('xwlMinHeight'), maxH = node.get('xwlMaxHeight'), ew = Ext
				.isEmpty(d.width), eh = Ext.isEmpty(d.height);
		if (!Ext.isEmpty(newId))
			id = newId;
		if (Ext.isEmpty(d.x))
			r.x = sx;
		else
			r.x = parseInt(d.x, 10);
		if (Ext.isEmpty(d.y))
			r.y = sy;
		else
			r.y = parseInt(d.y, 10);
		if (ew) {
			nw = node.get('xwlWidth');
			if (Ext.isEmpty(nw))
				nw = 100;
			else
				nw = parseInt(nw, 10);
			r.width = nw;
		} else
			r.width = parseInt(d.width, 10);
		if (eh) {
			nh = node.get('xwlHeight');
			if (Ext.isEmpty(nh))
				nh = 100;
			else
				nh = parseInt(nh, 10);
			r.height = parseInt(nh, 10);
		} else
			r.height = parseInt(d.height, 10);
		if (xtype == 'label') {
			r.value = d.text;
			r.hasText = true;
			xtype = 'textarea';
			r.fieldCls = 'ide_label';
			r.isLabel = true;
			if (!Ext.isEmpty(d.align)) {
				r.fieldStyle = 'text-align:' + d.align;
				r.fsSeted = true;
			}
			r.xwlId = id;
		} else if (Wb.indexOf(category, 'trigger') != -1) {
			r.value = id;
			r.allowBlank = false;
			xtype = 'trigger';
			r.validator = Ds.idValidatorBd;
			r.fieldStyle = 'cursor:move;';
		} else if (Wb.indexOf(category, 'text') != -1) {
			r.value = id;
			r.allowBlank = false;
			r.validator = Ds.idValidatorBd;
			if (xtype == 'textarea')
				r.fieldStyle = 'cursor:move;text-align:center;';
			else
				r.fieldStyle = 'cursor:move;';
			xtype = 'textfield';
		} else if (Wb.indexOf(category, 'button') != -1) {
			r.value = d.text;
			r.hasText = true;
			xtype = 'textfield';
			r.fieldCls = 'ide_button';
			r.xwlId = id;
		} else if (Wb.indexOf(category, 'grid') != -1) {
			r.value = id;
			r.allowBlank = false;
			r.validator = Ds.idValidatorBd;
			xtype = 'textfield';
			r.fieldStyle = 'background-image:url(webbuilder/images/app/grid.gif)';
			r.fieldCls = 'ide_grid';
		} else if (Wb.indexOf(category, 'date') != -1) {
			r.value = id;
			r.allowBlank = false;
			r.validator = Ds.idValidatorBd;
			xtype = 'textfield';
			r.fieldCls = 'ide_date';
		} else if (Wb.indexOf(category, 'check') != -1) {
			r.value = id;
			r.allowBlank = false;
			r.validator = Ds.idValidatorBd;
			xtype = 'textfield';
			r.fieldCls = 'ide_check';
		} else if (Wb.indexOf(category, 'radio') != -1) {
			r.value = id;
			r.allowBlank = false;
			r.validator = Ds.idValidatorBd;
			xtype = 'textfield';
			r.fieldCls = 'ide_radio';
		} else if (Wb.indexOf(category, 'image') != -1) {
			r.value = id;
			r.allowBlank = false;
			r.validator = Ds.idValidatorBd;
			xtype = 'textfield';
			r.fieldCls = 'ide_image';
			if (!Wb.isEmpty(d.src))
				r.fieldStyle = 'background-image:url(' + d.src + ');';
		} else {
			r.value = id;
			r.allowBlank = false;
			r.validator = Ds.idValidatorBd;
			xtype = 'textfield';
			r.fieldCls = 'ide_panel';
		}
		r.listeners = Ds.propEventHandle;
		r.xtype = xtype;
		r.bindNode = node;
		r.resizable = {
			widthIncrement : 8,
			heightIncrement : (r.height % 8 != 0 && !maxH) ? 2 : 8,
			listeners : {
				resizedrag : function(o, w, h, e) {
					this.elBox = Ext.fly(o.resizeTracker.proxy).getBox();
				},
				resize : function(o, w, h, e) {
					var d = o.el.id;
					o.el.setBox(this.elBox);
					Ds.setModified();
					Ds.updateAbsInfo(Wb.get(d.substring(0, d.length - 7)));
				}
			}
		};
		if (minW)
			r.minWidth = minW;
		if (maxW)
			r.maxWidth = maxW;
		if (minH)
			r.minHeight = minH;
		if (maxH)
			r.maxHeight = maxH;
		r.draggable = {
			listeners : {
				drag : Ds.dragHandle,
				mousedown : Ds.dragMousedown
			}
		};
		return r;
	},
	retModule : function() {
		var t = Ds.at();
		if (t && t.saveUrl)
			Ds.createTab(t.modTitle, t.modId, t.iconCls);
	},
	delayRun : function(isAll, func) {
		if (!Ds.delayRn)
			Ds.delayRn = new Ext.util.DelayedTask(Ds.run);
		Ds.delayRn.delay(102);
	},
	invoke : function(id, s, c) {
		var t = Pt.open(moduleTab, 'main?xwl=' + id, 'Run - ' + s, c);
		moduleTab.setActiveTab(t);
		t.modTitle = s;
		t.modId = id;
		Pt.load(t, true);
	},
	run : function() {
		var t = Ds.at(), a, b;
		if (!t) {
			Wb.warning('Please open a module to run.');
			return;
		}
		function execute() {
			if (t.saveUrl)
				Pt.load(t, true);
			else {
				a = t.title;
				b = t.id.substring(2);
				moduleTab.setActiveTab(Pt.open(moduleTab, 'main?xwl=' + b,
						'Run - ' + a, t.iconCls));
				t = Ds.at();
				t.modTitle = a;
				t.modId = b;
				Pt.load(t, true);
			}
		}
		Ds.completeEdit();
		if (t.isModified)
			Ds.saveModule(false, execute);
		else
			execute();
	},
	findTab : function(n) {
		var t = null;
		Ds.at().items.each(function(c) {
			if (c.bindObj == n && c.absPanel) {
				t = c;
				return false;
			}
		});
		return t;
	},
	getWidth : function(p, d) {
		var w;
		if (Ext.isEmpty(p.width))
			w = d;
		else
			w = parseInt(p.width, 10);
		return w;
	},
	getHeight : function(p, d) {
		var h;
		if (Ext.isEmpty(p.height))
			h = d;
		else
			h = parseInt(p.height, 10);
		return h;
	},
	editAbsLayout : function() {
		var az = Ds.at(), ax;
		if (!az || az.saveUrl) {
			Wb.warning('Please open a module to edit.');
			return;
		}
		ax = az.getActiveTab();
		if (!ax || ax !== az.generalTab) {
			Wb.warning('Please select a container control.');
			return;
		}
		function getItems() {
			var x = 8, y = 8, d = [];
			selObj.eachChild(function(n) {
				var p = n.get('properties')
				if (Ext.isEmpty(p.x) || Ext.isEmpty(p.y)) {
					x += 40;
					if (x > width - 80) {
						x = 8;
						y += 24;
					}
				}
				d.push(Ds.getRendProp(n, x, y));
			});
			return d;
		}

		var u, page = Ds.at(), pg = page.propertyGrid, selObj = page.objectTree.selectObject, objName = selObj
				.get('text'), prop = selObj.get('properties'), absTab = Ds
				.findTab(selObj), absTabObj, width = Ds.getWidth(prop, 450);

		if (!Ext.isDefined(prop.layout)) {
			Wb.warning(Wb.format('"{0}" has no layout.', objName));
			return;
		}
		if (prop.layout != 'absolute') {
			pg.setProperty('layout', 'absolute');
			Ds.setModified();
		}
		if (absTab) {
			page.setActiveTab(absTab);
			return;
		}
		page
				.add( {
					bindObj : selObj,
					iconCls : 'window_icon',
					title : objName,
					closable : true,
					autoScroll : true,
					bodyStyle : 'background-color:#787878',
					items : [ {
						width : width,
						height : Ds.getHeight(prop, 300),
						layout : 'absolute',
						xwlId : selObj.get('text'),
						resizable : {
							handles : 's e se',
							pinned : true,
							listeners : {
								resize : function(o, w, h, e) {
									if (absTabObj.idEdit.getValue() == absTabObj.absPanel.xwlId)
										Ds.updateAbsInfo(null);
									Ds.setModified();
								}
							}
						},
						plugins : [ new Wb.ide.DragSelector() ],
						xtype : 'panel',
						bodyStyle : 'background-image:url(webbuilder/images/app/dot.gif)',
						items : getItems(),
						bindNode : selObj,
						listeners : {
							render : function(t) {
								absTabObj.absPanel = t;
								t.body.on('dblclick', function(e, o) {
									if (Ext.EventObject.ctrlKey)
										return;
									var n, a = Ds.at();
									if (o.clientWidth != 5
											|| o.clientHeight != 5) {
										Ds.showGeneral();
										if (o == absTabObj.absPanel.body.dom)
											n = t.bindNode;
										else {
											n = Ds.findEl(t, o);
											if (n)
												n = n.bindNode;
											else
												n = null;
										}
										if (n)
											a.objectTree.selectPath(n
													.getPath('text'), 'text');
									}
								});
								t.body
										.on(
												'mousedown',
												function(e, o) {
													Ds.moduleSeled = false;
													if (o.clientWidth != 5
															|| o.clientHeight != 5) {
														if (o == absTabObj.absPanel.body.dom) {
															Ds.selectAll(t,
																	false);
															Ds
																	.updateAbsInfo(absTabObj.absPanel);
														} else
															Ds.selectEl(t, o,
																	e.shiftKey);
													}
													absTabObj.hEdit.focus(
															false, false);
													absTabObj.hEdit.blur();
												});
							}
						}
					} ],
					bbar : [ {
						text : 'id',
						xtype : 'tbtext'
					}, {
						xtype : 'textfield',
						width : 100,
						allowBlank : false,
						validator : Ds.idValidatorBd,
						listeners : {
							change : function(z, v) {
								Ds.setItemVal(function(o) {
									Ds.changeId(z.bindNode, z.getValue());
									if (o.xwlId)
										o.xwlId = v;
									else
										o.setValue(v);
								});
							},
							render : function(o) {
								absTabObj.idEdit = o;
							}
						}
					}, {
						text : 'text',
						xtype : 'tbtext'
					}, {
						xtype : 'textfield',
						width : 100,
						listeners : {
							change : function(o, v) {
								Ds.setItemVal(function(o) {
									o.setValue(v);
								});
							},
							render : function(o) {
								absTabObj.textEdit = o;
							}
						}
					}, {
						text : 'x',
						xtype : 'tbtext'
					}, {
						xtype : 'numberfield',
						width : 55,
						listeners : {
							change : function(o, v) {
								Ds.setItemVal(function(o) {
									Ext.fly(o.id + '-rzwrap').setLeft(v);
								});
							},
							render : function(o) {
								absTabObj.xEdit = o;
							}
						}
					}, {
						text : 'y',
						xtype : 'tbtext'
					}, {
						xtype : 'numberfield',
						width : 55,
						listeners : {
							change : function(o, v) {
								Ds.setItemVal(function(o) {
									Ext.fly(o.id + '-rzwrap').setTop(v);
								});
							},
							render : function(o) {
								absTabObj.yEdit = o;
							}
						}
					}, {
						text : 'w',
						xtype : 'tbtext'
					}, {
						xtype : 'numberfield',
						minValue : 0,
						width : 55,
						listeners : {
							change : function(o, v) {
								Ds.setItemVal(function(o) {
									u = Ext.fly(o.id + '-rzwrap');
									if (u)
										u.setWidth(v);
									o.setWidth(v);
								});
							},
							render : function(o) {
								absTabObj.wEdit = o;
							}
						}
					}, {
						text : 'h',
						xtype : 'tbtext'
					}, {
						xtype : 'numberfield',
						minValue : 0,
						width : 55,
						listeners : {
							change : function(o, v) {
								Ds.setItemVal(function(o) {
									u = Ext.fly(o.id + '-rzwrap');
									if (u)
										u.setHeight(v);
									o.setHeight(v);
								});
							},
							render : function(o) {
								absTabObj.hEdit = o;
							}
						}
					}, {
						iconCls : 'alignRight_icon',
						tooltip : 'Right align label',
						xtype : 'splitbutton',
						handler : function(o, p) {
							Ds.setTextAlign('right');
						},
						menu : {
							xtype : 'menu',
							plain : true,
							items : {
								xtype : 'buttongroup',
								title : 'Text alignment',
								columns : 4,
								height : 45,
								defaults : {
									xtype : 'button',
									scale : 'small'
								},
								items : [ {
									iconCls : 'alignLeft_icon',
									tooltip : 'Left align label',
									width : 28,
									handler : function() {
										Ds.setTextAlign('left');
									}
								}, {
									iconCls : 'alignCenter_icon',
									tooltip : 'Center align label',
									width : 28,
									handler : function() {
										Ds.setTextAlign('center');
									}
								}, {
									iconCls : 'alignRight_icon',
									tooltip : 'Right align label',
									width : 28,
									handler : function() {
										Ds.setTextAlign('right');
									}
								}, {
									iconCls : 'alignRightPin_icon',
									enableToggle : true,
									pressed : false,
									width : 28,
									tooltip : 'Toggle right align new label',
									toggleHandler : function(o, p) {
										absTabObj.labelRight = p;
									}
								} ]
							}
						}
					}, {
						iconCls : 'align_icon',
						tooltip : 'Align controls',
						menu : {
							xtype : 'menu',
							plain : true,
							items : {
								xtype : 'buttongroup',
								title : 'Align controls',
								columns : 4,
								height : 93,
								defaults : {
									xtype : 'button',
									scale : 'large'
								},
								items : [ {
									iconCls : 'ide_h1',
									width : 35,
									height : 35,
									tooltip : 'Align left edges',
									handler : function() {
										Ds.setAlign(1);
									}
								}, {
									iconCls : 'ide_h2',
									width : 35,
									height : 35,
									tooltip : 'Align horizontal centers',
									handler : function() {
										Ds.setAlign(2);
									}
								}, {
									iconCls : 'ide_h3',
									width : 35,
									height : 35,
									tooltip : 'Center horizontally in window',
									handler : function() {
										Ds.setAlign(3);
									}
								}, {
									iconCls : 'ide_h4',
									width : 35,
									height : 35,
									tooltip : 'Align right edges',
									handler : function() {
										Ds.setAlign(4);
									}
								}, {
									iconCls : 'ide_v1',
									width : 35,
									height : 35,
									tooltip : 'Align tops',
									handler : function() {
										Ds.setAlign(5);
									}
								}, {
									iconCls : 'ide_v2',
									width : 35,
									height : 35,
									tooltip : 'Align vertical centers',
									handler : function() {
										Ds.setAlign(6);
									}
								}, {
									iconCls : 'ide_v3',
									width : 35,
									height : 35,
									tooltip : 'Center vertically in window',
									handler : function() {
										Ds.setAlign(7);
									}
								}, {
									iconCls : 'ide_v4',
									width : 35,
									height : 35,
									tooltip : 'Align bottoms',
									handler : function() {
										Ds.setAlign(8);
									}
								} ]
							}
						}
					} ],
					listeners : {
						destroy : function(t) {
							Wb.closeNav(backBtn, forwardBtn, t);
						},
						render : function(t) {
							absTabObj = t;
							t.commitChange = function() {
								Ds.setAbsLayout(t.absPanel);
							}
						}
					}
				});
		page.setActiveTab(absTabObj);
		Ds.setDDSupport(absTabObj.absPanel);
		Ds.selectAll(absTabObj.absPanel, false);
		absTabObj.on('activate', Ds.syncTree);
	},
	syncTree : function(t) {
		Ds.moduleSeled = false;
		if (Ds.stopSync)
			return;
		var p = t.absPanel, n = p.bindNode, r = n.get('properties'), z, sel = null, ct = 0, x = 8, y = 8, l, m, w = p
				.getWidth(), nw, nh;
		p.setIconCls(r.iconCls);
		p.setWidth(Ds.getWidth(r, 450));
		p.setHeight(Ds.getHeight(r, 300));
		p.xwlId = n.get('text');
		p.items.each(function(c) {
			n = c.bindNode;
			mt = n.get('xwlMeta');
			r = n.get('properties');
			if (mt == 'label') {
				c.inputEl.setStyle('text-align', r.align);
				c.fsSeted = !Wb.isEmpty(r.align);
			}
			if (mt == 'image') {
				if (Wb.isEmpty(r.src))
					z = 'webbuilder/images/null.gif';
				else
					z = r.src;
				c.inputEl.setStyle('background-image', 'url(' + z + ')');
			}
			if (c.xwlId)
				c.xwlId = r.id;
			if (c.hasText)
				c.setValue(r.text);
			else
				c.setValue(r.id);
			if (Ext.isEmpty(r.x) || Ext.isEmpty(r.y)) {
				x += 40;
				if (x > w - 80) {
					x = 8;
					y += 24;
				}
			}
			if (Ext.isEmpty(r.x))
				l = x;
			else
				l = parseInt(r.x, 10);
			if (Ext.isEmpty(r.y))
				m = y;
			else
				m = parseInt(r.y, 10);
			if (Ext.isEmpty(r.width)) {
				nw = n.get('xwlWidth');
				if (Ext.isEmpty(nw))
					nw = 100;
				else
					nw = parseInt(nw, 10);
			} else
				nw = parseInt(r.width, 10);
			if (Ext.isEmpty(r.height)) {
				nh = n.get('xwlHeight');
				if (Ext.isEmpty(nh))
					nh = 100;
				else
					nh = parseInt(nh, 10);
			} else
				nh = parseInt(r.height, 10);
			Ds.ssz(c, l, m, nw, nh);
			if (c.ideSel) {
				if (sel == null)
					sel = c;
				ct++;
			}
		});
		Ds.updateAbsInfo(sel, ct);
	},
	setAbsLayout : function(p) {
		var n = Ds.at().objectTree.selectObject, g = Ds.at().propertyGrid, r, l, u;

		function setLay(c, setP) {
			u = Ds.gxy(c);
			if (n == c.bindNode) {
				if (setP) {
					g.setProperty('x', u[0]);
					g.setProperty('y', u[1]);
				}
				g.setProperty('width', c.getWidth());
				g.setProperty('height', c.getHeight());
				if (c.xwlId) {
					g.setProperty('id', c.xwlId);
					c.bindNode.set('text', c.xwlId);
					if (c.isLabel) {
						l = c.inputEl.getStyle('text-align');
						if (!c.fsSeted)
							l = '';
						g.setProperty('align', l);
					}
				} else {
					g.setProperty('id', c.getValue());
					c.bindNode.set('text', c.getValue());
				}
				c.bindNode.commit();
				if (c.hasText)
					g.setProperty('text', c.getValue());
			} else {
				r = c.bindNode.get('properties');
				if (setP) {
					r.x = u[0];
					r.y = u[1];
				}
				r.width = c.getWidth();
				r.height = c.getHeight();
				if (c.xwlId) {
					r.id = c.xwlId;
					if (c.isLabel) {
						l = c.inputEl.getStyle('text-align');
						if (!c.fsSeted)
							l = '';
						r.align = l;
					}
				} else
					r.id = c.getValue();
				if (c.hasText)
					r.text = c.getValue();
			}
		}
		setLay(p, false);
		p.items.each(function(c) {
			setLay(c, true);
		});
	},
	setItemVal : function(func) {
		if (Ds.stopSetProp)
			return;
		var b = true, f = Ds.pn();
		f.items.each(function(c) {
			if (c.ideSel) {
				Ds.stopRecChg = true;
				func(c);
				Ds.stopRecChg = false;
				b = false;
			}
		});
		Ds.setModified();
		if (b)
			func(f);
	},
	findObj : function(p, obj) {
		var o = null;
		p.items.each(function(c) {
			if (c.bindNode == obj) {
				o = c;
				return false;
			}
		});
		return o;
	},
	delBdObj : function(n) {
		Ds.at().items.each(function(c) {
			if (c.bindObj == n)
				c.close();
		});
		var p = n.parentNode, d, t;
		t = Ds.findTab(p);
		if (t) {
			d = t.absPanel;
			if (d) {
				var o = Ds.findObj(d, n);
				if (o)
					d.remove(o, true);
			}
		}
		n.eachChild(function(c) {
			Ds.delBdObj(c);
		});
	},
	commitSet : function() {
		if (Ds.isFolder === 'property')
			Ds.setModule();
		else
			Ds.newModule();
	},
	delPdObj : function(p, id) {
		var t = Ds.findTab(p), d;
		if (t) {
			d = t.absPanel;
			d.items
					.each(function(c) {
						if (c.xwlId == id || Ext.isEmpty(c.xwlId)
								&& c.getValue() == id)
							d.remove(c, true);
					});
		}
	},
	removeChildren : function() {
		if (Ds.slModule())
			Ds.delModule(false);
		else {
			var page = Ds.at(), node = page.objectTree.selectObject, c = node.childNodes, i, j = c.length;
			for (i = j - 1; i >= 0; i--) {
				Ds.delBdObj(c[i]);
				c[i].remove();
			}
			if (j > 0)
				Ds.setModified();
		}
	},
	removeNode : function() {
		var page = Ds.at(), node = page.objectTree.selectObject;
		if (node && node.getDepth() > 1) {
			if (node.nextSibling)
				page.objectTree.view.select(node.nextSibling);
			else if (node.previousSibling)
				page.objectTree.view.select(node.previousSibling);
			else
				page.objectTree.view.select(node.parentNode);
			Ds.delBdObj(node);
			node.remove();
			Ds.setModified();
		} else
			Wb.warning(Wb.format(Str.cannotDelete, node.get('text')));
	},
	controlStoreLoad : function() {
		var t = controlTree, n = t.getRootNode().firstChild;
		t.view.select(n);
		n.expand();
	},
	moduleTreeConfig : function() {
		return {
			plugins : {
				pluginId : 'ddPlug',
				ptype : 'treeviewdragdrop',
				ddGroup : 'modules'
			},
			listeners : {
				beforedrop : function(node, data, om, dp) {
					if (!Wb.getNode(node) || !Wb.getNode(data.item))
						return false;
					Wb.saveNodePos(Wb.getNode(data.item));
					return Wb.getTree(data.item) == moduleTree;
				},
				drop : Ds.saveModuleTree
			}
		}
	},
	controlTreeConfig : function() {
		return {
			plugins : {
				pluginId : 'ddPlug',
				ptype : 'treeviewdragdrop',
				ddGroup : 'controlList',
				enableDrop : false
			}
		}
	},
	removeFmControl : function() {
		var at = Ds.at(), ca = at.getActiveTab();
		var p = ca.absPanel, b = false, n;

		at.setActiveTab(at.generalTab);
		p.items.each(function(c) {
			if (c.ideSel) {
				n = c.bindNode;
				Ds.delBdObj(c.bindNode);
				n.remove();
				b = true;
			}
		});
		Ds.stopSync = true;
		Ds.at().setActiveTab(ca);
		Ds.stopSync = false;
		if (b) {
			Ds.updateAbsInfo(null);
			Ds.at().objectTree.view.select(p.bindNode);
			Ds.setModified();
		}
	},
	removeControl : function() {
		if (Ds.slModule())
			Ds.delModule(true);
		else {
			var t = Ds.at();
			if (t.getActiveTab() == t.generalTab)
				Ds.removeNode();
			else
				Ds.removeFmControl();
		}
	},
	cloneNode : function(c) {
		var d = {};
		if (c.data.children)
			c.data.children = undefined;
		d.n = c.copy(Ds.getId());
		if (c.hasChildNodes()) {
			var l = [];
			c.eachChild(function(x) {
				l.push(Ds.cloneNode(x));
			});
			d.c = l;
		}
		return d;
	},
	copyNode : function() {
		var n = Ds.getSelNode();
		if (n)
			Ds.clipBoard = [ Ds.cloneNode(n) ];
	},
	copyFmControl : function() {
		var p = Ds.pn(), l = [], b = false;
		Ds.at().getActiveTab().commitChange();
		p.items.each(function(o) {
			if (o.ideSel) {
				l.push(Ds.cloneNode(o.bindNode));
				b = true;
			}
		});
		if (b)
			Ds.clipBoard = l;
	},
	copyControl : function() {
		if (Ds.slModule())
			Ds.copyModule(false);
		else {
			var t = Ds.at().getActiveTab();
			if (t == Ds.at().generalTab)
				Ds.copyNode();
			else
				Ds.copyFmControl();
		}
	},
	cutNode : function() {
		Ds.copyControl();
		Ds.removeControl();
	},
	cutFmControl : function() {
		Ds.copyFmControl();
		Ds.removeFmControl();
	},
	slModule : function() {
		var t = Ds.at();
		return Ds.moduleSeled
				|| t
				&& (t.saveUrl || t && t.getActiveTab()
						&& t.getActiveTab().isScriptEdt)
				|| moduleTab.items.length == 0;
	},
	cutControl : function() {
		if (Ds.slModule())
			Ds.cutModule();
		else {
			var t = Ds.at().getActiveTab();
			if (t == Ds.at().generalTab)
				Ds.cutNode();
			else
				Ds.cutFmControl();
		}
	},
	appendNode : function(x, d, b) {
		var y, w, i, j;
		if (b) {
			w = d.n.copy(Ds.getId());
			y = x.appendChild(w);
		} else
			y = x;
		Ds.renameNode(y);
		if (d.c) {
			j = d.c.length;
			for (i = 0; i < j; i++)
				Ds.appendNode(y, d.c[i], true);
		}
	},
	pasteNode : function(shiftFlg) {
		var shift, n = Ds.getSelNode();
		if (n == null || Ds.clipBoard == null)
			return;
		var p = n.parentNode, s = n.nextSibling, c, i = 0, j = Ds.clipBoard.length, fn = null, cd, absP = null, t, g, r, no;
		if (n.getDepth() == 1)
			shift = true;
		else
			shift = shiftFlg;
		if (Ds.clipBoard[0].n.get('xwlMeta') == 'module') {
			t = Ds.at();
			g = t.generalTab;
			t.items.each(function(c) {
				if (c != g)
					c.close();
			});
			r = t.objectTree.getRootNode();
			r.firstChild.remove();
			n = r;
			shift = true;
		} else {
			if (shift)
				cd = n;
			else
				cd = p;
			if (!Ds.canDroped(Ds.clipBoard[0].n, cd, true)) {
				shift = true;
				cd = n;
				if (!Ds.canDroped(Ds.clipBoard[0].n, cd, true)) {
					Wb.warning(Str.cannotAppend);
					return;
				}
			}
		}
		for (i = 0; i < j; i++) {
			c = Ds.clipBoard[i].n.copy(Ds.getId());
			if (shift)
				c = n.appendChild(c);
			else if (!s)
				c = p.appendChild(c);
			else
				c = p.insertBefore(c, s);
			absP = Ds.findTab(c.parentNode);
			if (fn == null)
				fn = c;
			Ds.appendNode(c, Ds.clipBoard[i], false);
			if (absP) {
				no = absP.absPanel.add(Ds.getRendProp(c, 8, 8, c.get('text')));
				Ds.selectObj(no, false);
			}
		}
		if (fn) {
			fn.parentNode.expand();
			Ds.at().objectTree.view.select(fn);
		}
		Ds.setModified();
	},
	pasteFmControl : function() {
		if (Ds.clipBoard == null)
			return;
		var p = Ds.pn(), n = p.bindNode, i = 0, j = Ds.clipBoard.length, c, x = 8, y = 8, d = [], z, width = p
				.getWidth(), pasteNd, exp;

		if (!Ds.canDroped(Ds.clipBoard[0].n, n, true)) {
			Wb.warning(Str.cannotAppend);
			return;
		}
		for (i = 0; i < j; i++) {
			c = Ds.clipBoard[i].n.copy(Ds.getId());
			c = n.appendChild(c);
			Ds.appendNode(c, Ds.clipBoard[i], false);
			z = c.get('properties')
			if (Ext.isEmpty(z.x) || Ext.isEmpty(z.y)) {
				x += 40;
				if (x > width - 80) {
					x = 8;
					y += 24;
				}
			}
			exp = Ds.getRendProp(c, x, y, c.get('text'));
			exp.newPaste = true;
			d.push(exp);
		}
		p.add(d);
		Ds.selectAll(p, false);
		pasteNd = null;
		p.items.each(function(cd) {
			if (cd.newPaste) {
				cd.newPaste = false;
				if (pasteNd == null)
					pasteNd = cd;
				Ds.selectObj(cd, true);
			}
		});
		Ds.updateAbsInfo(pasteNd, j);
		Ds.setModified();
	},
	pasteControl : function(cd) {
		var isAppend = Ext.EventObject.shiftKey || cd;
		if (Ds.slModule())
			Ds.pasteModule(isAppend);
		else {
			var t = Ds.at().getActiveTab();
			if (t == Ds.at().generalTab)
				Ds.pasteNode(isAppend);
			else
				Ds.pasteFmControl();
		}
	},
	restoreIcon : function() {
		var i, j = Ds.iconList.length, s;
		for (i = 0; i < j; i++) {
			s = Ds.iconList[i];
			Ds.iconList[i] = [ s + '_icon', s + '_icon', s ];
		}
	},
	setDisplay : function(n) {
		var m = n.get('xwlCategory');
		if (Wb.indexOf(m, 'labelComp') != -1) {
			var p = n.get('properties');
			p.text = n.get('text');
			if (n.get('xwlMeta') == 'label'
					&& Ds.at().getActiveTab().labelRight)
				p.align = 'right';
		}
	},
	addNode : function(node) {
		var n = Ds.getSelNode(), x;
		if (n == null)
			return;
		var p = n.parentNode, s = n.nextSibling, z, b1, b2, absP = null;
		b1 = !Ext.EventObject.ctrlKey && Ds.canDroped(node, p, true);
		b2 = Ds.canDroped(node, n, true);
		if (!b1 && !b2) {
			Wb.warning(Str.cannotAppend);
			return;
		}
		x = node.copy(Ds.getId());
		if (b1) {
			if (s)
				z = p.insertBefore(x, s);
			else
				z = p.appendChild(x);
			absP = Ds.findTab(p);
		} else {
			z = n.appendChild(x);
			absP = Ds.findTab(n);
		}
		Ds.renameNode(z, true);
		Ds.setDisplay(z);
		if (absP) {
			var no = absP.absPanel.add(Ds.getRendProp(z, 8, 8, z.get('text')));
			Ds.selectObj(no, false);
		}
		z.parentNode.expand();
		Ds.at().objectTree.view.select(z);
		Ds.setModified();
	},
	addFmControl : function(r) {
		var panel = Ds.pn();
		if (!Ds.canDroped(r, panel.bindNode, true)) {
			Wb.warning(Str.cannotAppend);
			return;
		}
		var x = r.copy(Ds.getId()), node = panel.bindNode.appendChild(x), id, o;
		Ds.renameNode(node, true);
		Ds.setDisplay(node);
		id = node.get('text');
		Ds.selectAll(panel, false);
		o = panel.add(Ds.getRendProp(node, 16, 16, id));
		o.ideSel = true;
		Ds.updateAbsInfo(o);
		Ds.setModified();
	},
	addControl : function(r) {
		var t = Ds.at();
		if (!t || t.saveUrl)
			return;
		t = t.getActiveTab();
		if (t == Ds.at().generalTab)
			Ds.addNode(r);
		else {
			if (t.absPanel) {
				Ds.addFmControl(r);
			}
		}
	},
	setDDSupport : function(panel) {
		new Ext.dd.DropTarget(
				panel.body.dom,
				{
					ddGroup : 'controlList',
					notifyDrop : function(ddSource, e, data) {
						if (!Ds
								.canDroped(data.records[0], panel.bindNode,
										true)) {
							Wb.warning(Str.cannotAppend);
							return;
						}
						Ds.selectAll(panel, false);
						var x = e.getX() - panel.body.getLeft(), y = e.getY()
								- panel.body.getTop(), rd = data.records[0], cp = rd
								.copy(Ds.getId()), o, nd = Ds.at()
								.getActiveTab().bindObj.appendChild(cp);
						Ds.renameNode(nd, true);
						Ds.setDisplay(nd);
						o = panel.add(Ds.getRendProp(nd, Ds.getSnap(x), Ds
								.getSnap(y), nd.get('text')));
						o.ideSel = true;
						Ds.updateAbsInfo(o);
						Ds.setModified();
						return true;
					}
				});
	},
	defDragSel : function() {
		Ext
				.define(
						'Wb.ide.DragSelector',
						{
							requires : [ 'Ext.dd.DragTracker',
									'Ext.util.Region' ],
							init : function(panel) {
								this.panel = panel;
								panel.mon(panel, {
									beforecontainerclick : this.cancelClick,
									scope : this,
									render : {
										fn : this.onRender,
										scope : this,
										single : true
									}
								});
							},
							onRender : function() {
								this.tracker = new Ext.dd.DragTracker( {
									panel : this.panel,
									el : this.panel.body,
									dragSelector : this,
									onBeforeStart : this.onBeforeStart,
									onStart : this.onStart,
									onDrag : this.onDrag,
									onEnd : this.onEnd
								});
								this.dragRegion = new Ext.util.Region();
							},
							onBeforeStart : function(e) {
								return e.target == this.panel.body.dom;
							},
							onStart : function(e) {
								var dragSelector = this.dragSelector;
								this.dragging = true;
								dragSelector.fillRegions();
								dragSelector.getProxy().show();
							},
							cancelClick : function() {
								return !this.tracker.dragging;
							},
							onDrag : function(e) {
								var dragSelector = this.dragSelector, dragRegion = dragSelector.dragRegion, bodyRegion = dragSelector.bodyRegion, proxy = dragSelector
										.getProxy(), startXY = this.startXY, currentXY = this
										.getXY(), minX = Math.min(startXY[0],
										currentXY[0]), minY = Math.min(
										startXY[1], currentXY[1]), width = Math
										.abs(startXY[0] - currentXY[0]), height = Math
										.abs(startXY[1] - currentXY[1]), region, selected;

								Ext.apply(dragRegion, {
									top : minY,
									left : minX,
									right : minX + width,
									bottom : minY + height
								});

								dragRegion.constrainTo(bodyRegion);
								proxy.setRegion(dragRegion);
								var seled = null, ct = 0;
								this.panel.items.each(function(c) {
									region = c.el.getRegion();
									selected = dragRegion.intersect(region);
									Ds.selectObj(c, selected);
									if (selected) {
										seled = c;
										ct++;
									}
								});
								Ds.updateAbsInfo(seled, ct);
							},
							onEnd : Ext.Function.createDelayed(function(e) {
								var dragSelector = this.dragSelector;
								this.dragging = false;
								dragSelector.getProxy().hide();
							}, 1),
							getProxy : function() {
								if (!this.proxy) {
									this.proxy = this.panel.body.createChild( {
										tag : 'div',
										cls : 'x-view-selector'
									});
								}
								return this.proxy;
							},
							fillRegions : function() {
								var panel = this.panel;
								this.bodyRegion = panel.body.getRegion();
							}
						});
	},
	initialize : function() {
		Ds.defDragSel();
		Ds.defColorPicker();
		Ds.modifyExt();
		Wd.WBXwlPrint = Ds.print;
	},
	print : function(s) {
		Ds.consoleRaws.push(Ds.consoleIdx);
		Ext.DomHelper.insertHtml('beforeEnd', consolePanel.body.dom,
				'<span id="xln' + (Ds.consoleIdx++) + '">' + Wb.toHtml('' + s)
						+ '</span>');
		if (Ds.consoleRaws.length > 200) {
			Ext.fly('xln' + Ds.consoleRaws[0]).remove();
			Ds.consoleRaws.splice(0, 1);
		}
		consolePanel.body.dom.scrollTop = consolePanel.body.dom.scrollHeight;
	},
	hideView : function() {
		Ds.stopConsole();
		viewTab.hide();
	},
	showView : function(i) {
		viewTab.show();
		if (i !== null)
			viewTab.setActiveTab(i);
		if (viewTab.getActiveTab() == consolePanel)
			Ds.startConsole();
	},
	stopConsole : function() {
		if (Ds.consoleTime !== null) {
			clearInterval(Ds.consoleTimer);
			Ds.consoleTimer = null;
		}
	},
	startConsole : function() {
		if (Ds.consoleTimer == null) {
			Ds.consoleTimer = setInterval(function() {
				if (!Ds.consolePaused)
					Ds.queryConsole();
			}, 2000);
		}
	},
	clearConsole : function() {
		var c = Ds.consoleRaws, i, j = c.length, o;
		for (i = j - 1; i >= 0; i--) {
			o = Ext.fly('xln' + c[i]);
			if (o)
				o.remove();
		}
		c.splice(0, j);
	},
	queryConsole : function() {
		if (Ds.reqConsole)
			return false;
		Ds.reqConsole = true;
		Ext.Ajax.request( {
			url : "main?xwl=13MMOY5MGR78",
			timeout : 10000,
			callback : function() {
				Ds.reqConsole = false;
			},
			success : function(r) {
				var i = 0, j, s = r.responseText;
				if (Wb.isEmpty(s))
					return;
				j = s.length;
				while (i < j) {
					Ds.print(s.substring(i, i + 200));
					i += 200;
				}
			}
		});
	},
	modifyExt : function() {
		Ds.saveComboQuery = Ext.form.field.ComboBox.prototype.doRawQuery;
		Ext.form.field.ComboBox.prototype.doRawQuery = function() {
			var k = String.fromCharCode(Ds.keyCode);
			if (Ds.ctrlKey && (k == 'S' || k == 'Q'))
				return;
			Ds.saveComboQuery.call(this);
		};
		Ds.saveComboGV = Ext.form.field.ComboBox.prototype.getValue;
		Ext.form.field.ComboBox.prototype.getValue = function() {
			var v = Ds.saveComboGV.call(this);
			if (Ext.isArray(v))
				v = v.join(',');
			return v || '';
		};
		Ext
				.override(
						Ext.grid.plugin.CellEditing,
						{
							cancelEdit : function() {
								var me = this, activeEd = me.getActiveEditor(), viewEl = me.grid
										.getView().getEl(me.getActiveColumn());
								if (activeEd
										&& activeEd.field.lastQuery !== activeEd.field.lastValue)
									activeEd.field.fireEvent('change',
											activeEd.field);
								me.setActiveEditor(null);
								me.setActiveColumn(null);
								me.setActiveRecord(null);
								if (activeEd) {
									activeEd.cancelEdit();
									viewEl.focus();
									me.callParent(arguments);
								}
							}
						});
	},
	selectAll : function(p, b) {
		var o = null, i = 0;
		p.items.each(function(c) {
			if (o == null)
				o = c;
			i++;
			Ds.selectObj(c, b);
		});
		if (b)
			Ds.updateAbsInfo(o, i);
		else
			Ds.updateAbsInfo(null);
	},
	findEl : function(p, e) {
		var o = null;
		p.items.each(function(c) {
			if (c.el.dom == e || c.inputEl && c.inputEl.dom == e) {
				o = c;
				return false;
			}
		});
		return o;
	},
	selectEl : function(p, e, b) {
		var o = Ds.findEl(p, e), ct = 0, sel = true;
		if (o == null)
			return;
		if (b) {
			Ds.selectObj(o, !o.ideSel);
			sel = false;
		} else if (o.ideSel)
			sel = false;
		p.items.each(function(c) {
			if (sel)
				Ds.selectObj(c, c.el.dom == e || c.inputEl
						&& c.inputEl.dom == e);
			if (c.ideSel)
				ct++;
		});
		Ds.updateAbsInfo(o, ct);
	},
	pn : function() {
		return Ds.at().getActiveTab().absPanel;
	},
	updateAbsInfo : function(obj, len) {
		Ds.stopSetProp = true;
		var o, ct, t = Ds.at().getActiveTab(), u;
		if (Ext.isEmpty(len))
			ct = 1;
		else
			ct = len;
		if (obj == null && ct == 1 || ct == 0) {
			o = Ds.pn();
			ct = 1;
		} else
			o = obj;
		t.idEdit.setDisabled(ct > 1);
		if (o == null || ct != 1) {
			if (ct > 1)
				t.idEdit.setValue('(' + ct + ' items)');
			else
				t.idEdit.setValue('');
			t.textEdit.setValue('');
			t.textEdit.setDisabled(true);
			t.xEdit.setDisabled(false);
			t.yEdit.setDisabled(false);
			t.xEdit.setValue('');
			t.yEdit.setValue('');
			t.wEdit.setValue('');
			t.hEdit.setValue('');
		} else {
			t.idEdit.bindNode = o.bindNode;
			if (o.xwlId)
				t.idEdit.setValue(o.xwlId);
			else
				t.idEdit.setValue(o.getValue());
			t.textEdit.setDisabled(!o.hasText);
			if (!o.hasText)
				t.textEdit.setValue('');
			if (o.hasText)
				t.textEdit.setValue(o.getValue());
			u = Ds.gxy(o);
			t.xEdit.setValue(u[0]);
			t.yEdit.setValue(u[1]);
			t.xEdit.setDisabled(o == t.absPanel);
			t.yEdit.setDisabled(o == t.absPanel);
			t.wEdit.setValue(o.width);
			t.hEdit.setValue(o.height);
		}
		Ds.stopSetProp = false;
	},
	selectObj : function(c, b) {
		if (c.ideSel != null && c.ideSel == b)
			return;
		var r = c.resizer;
		if (r) {
			var p = r.possiblePositions, n;
			c.ideSel = b;
			for (n in p) {
				var d = r[p[n]];
				if (d)
					b ? d.show() : d.hide();
			}
		}
	},
	defColorPicker : function() {
		Ext.define('Wb.ide.ColorField', {
			extend : 'Ext.form.field.Picker',
			requires : [ 'Ext.picker.Color' ],
			matchFieldWidth : false,
			initComponent : function() {
				var me = this;
				me.callParent();
			},
			getErrors : function(value) {
				return [];
			},
			createPicker : function() {
				var me = this;
				me.picker = new Ext.picker.Color( {
					ownerCt : me.ownerCt,
					renderTo : document.body,
					floating : true,
					hidden : true,
					focusOnShow : true,
					listeners : {
						scope : me,
						select : me.onSelect
					},
					keyNavConfig : {
						esc : function() {
							me.collapse();
						}
					}
				});
				return me.picker;
			},
			onSelect : function(m, d) {
				var me = this;
				me.setValue('#' + d);
				me.collapse();
			},
			onExpand : function() {
				var me = this, v = me.getValue();
				if (!Ext.isEmpty(v) && v.substring(0, 1) == '#') {
					v = v.substring(1);
					if (Wb.indexOf(me.picker.colors, v) != -1)
						me.picker.select(v, true);
				}
			},
			onCollapse : function() {
				this.focus(false, 60);
			},
			beforeBlur : function() {
				var me = this, v = me.getValue(), focusTask = me.focusTask;
				if (focusTask) {
					focusTask.cancel();
				}
				if (v) {
					me.setValue(v);
				}
			}
		});
	},
	setAlign : function(type) {
		var u, w, h, p = Ds.pn(), tl, tr, tw, ml = null, mr = null, mw = null, fw = p
				.getWidth(), offL, flg = false, tt, tb, th, mt = null, mb = null, mh = null, fh = p
				.getHeight()
				- (p.header ? p.header.getHeight() : 0);
		p.items.each(function(o) {
			if (o.ideSel) {
				u = Ds.gxy(o);
				w = o.getWidth();
				h = o.getHeight();
				tl = u[0];
				if (ml === null || tl < ml)
					ml = tl;
				tr = u[0] + w;
				if (mr === null || tr > mr)
					mr = tr;
				tw = w;
				if (mw === null || tw > mw)
					mw = tw;
				tt = u[1];
				if (mt === null || tt < mt)
					mt = tt;
				tb = u[1] + h;
				if (mb === null || tb > mb)
					mb = tb;
				th = h;
				if (mh === null || th > mh)
					mh = th;
			}
		});
		offL = (fw - mr + ml) / 2 - ml;
		offH = (fh - mb + mt) / 2 - mt;
		p.items.each(function(o) {
			if (o.ideSel) {
				flg = true;
				u = Ext.fly(o.id + '-rzwrap');
				w = Ds.gxy(o);
				switch (type) {
				case 1:
					u.setLeft(ml);
					break;
				case 2:
					u.setLeft(ml + (mw - o.getWidth()) / 2);
					break;
				case 3:
					u.setLeft(w[0] + offL);
					break;
				case 4:
					u.setLeft(mr - o.getWidth());
					break;
				case 5:
					u.setTop(mt);
					break;
				case 6:
					u.setTop(mt + (mh - o.getHeight()) / 2);
					break;
				case 7:
					u.setTop(w[1] + offH);
					break;
				case 8:
					u.setTop(mb - o.getHeight());
					break;
				}
			}
		});
		if (flg)
			Ds.setModified();
	},
	setTextAlign : function(align) {
		var p = Ds.pn(), flg = false;
		p.items.each(function(o) {
			if (o.ideSel && o.isLabel) {
				o.inputEl.setStyle('text-align', align);
				o.fsSeted = true;
				flg = true;
			}
		});
		if (flg)
			Ds.setModified();
	},
	cutModule : function() {
		Ds.copyModule(true);
	},
	copyModule : function(isCut) {
		Ds.isCut = isCut;
		var m = moduleTree.selectModule;
		Ds.copiedModule = m;
	},
	pasteModule : function(isAppend) {
		var t = moduleTree;
		if (t.pasting)
			return;
		if (!Ds.copiedModule || !t.view.getNode(Ds.copiedModule)) {
			Wb.warning('Source module does not exist.');
			return;
		}
		var v, x = Ds.copiedModule.copy(Ds.getId()), m = t.selectModule, p;
		if (m.get('IS_FOLDER')) {
			if (Ds.isCut)
				isAppend = true;
		} else
			isAppend = false;
		if (isAppend)
			p = m;
		else
			p = m.parentNode;
		v = p;
		while (v) {
			if (v == Ds.copiedModule) {
				Wb
						.warning('The destination folder is a subfolder of the source folder.');
				return;
			}
			v = v.parentNode;
		}
		p.expand(false, function() {
			var oi = p.lastChild ? p.lastChild.get('ORDER_INDEX') + 1 : 1;
			t.pasting = true;
			Wb.request( {
				url : 'main?xwl=13L88K98GZOG',
				mask : t,
				params : {
					isCut : Ds.isCut,
					id : x.get('MODULE_ID'),
					parentId : p.getDepth() == 0 ? -1 : p.get('MODULE_ID'),
					orderIndex : oi
				},
				callback : function() {
					t.pasting = false;
				},
				success : function(resp) {
					var o = Ext.decode(resp.responseText);
					x.set('loaded', false);
					if (!Ds.isCut) {
						x.set('MODULE_ID', o.id);
						x.set('CREATE_DATE', o.date);
						x.set('LAST_MODIFY_DATE', o.date);
						x.set('CREATE_USER', o.user);
						x.set('LAST_MODIFY_USER', o.user);
					}
					x.set('ORDER_INDEX', oi);
					x.commit();
					var n = p.appendChild(x);
					t.view.select(n);
					if (Ds.isCut) {
						Ds.copiedModule.remove();
						Ds.copiedModule = null;
					}
				}
			});
		});
	},
	dropUrl : function(g) {
		new Ext.dd.DropTarget(g.body.dom, {
			ddGroup : 'modules',
			notifyDrop : function(s, e, d) {
				var x = g.view, v = x.cellSelector, c = e
						.getTarget(v.cellSelector), l, r, n;
				if (c) {
					l = x.findItemByChild(c), r = d.records[0], n = x
							.getRecord(l).get('name');
					if (Wb.getNamePart(
							Ds.at().objectTree.selectObject.get('xwlPT')[n])
							.substring(0, 3) == 'url'
							&& !r.get('IS_FOLDER')) {
						g.setProperty(n, '#' + r.get('MODULE_ID') + ' ('
								+ r.get('orgText') + ')');
						Ds.setModified();
					}
				}
			}
		});
	}
};