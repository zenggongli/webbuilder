var Cm = {
	at : function() {
		return controlTab.getActiveTab()
	},
	editWin : null,
	clipBoard : null,
	objectTreeFields : [ 'text', 'xwlMeta', 'id', 'properties', 'events',
			'custGEditors', 'custPEditors', 'custEEditors', 'custEPara' ],
	setButtons : function(b) {
		appendBtn.setDisabled(b);
		delBtn.setDisabled(b);
		copyBtn.setDisabled(b);
		pasteBtn.setDisabled(b);
	},
	findTab : function(id) {
		var r = null;
		controlTab.items.each(function(c) {
			if (c.bindId == id) {
				r = c;
				return false;
			}
		});
		return r;
	},
	finalize : function() {
		Ext.getDoc().on('keydown', Cm.monitorElKey);
	},
	createTab : function(title, id, iconCls, content) {
		var tab = controlTab;
		var page, fnT = Cm.findTab(id);

		if (fnT) {
			tab.setActiveTab(fnT);
			return;
		}
		function loadData(d) {
			Wb.decodeValue(d.custGEditors);
			Wb.decodeValue(d.custPEditors);
			Wb.decodeValue(d.custEEditors);
			var g = page.generalGrid;
			var p = page.propertyGrid;
			var e = page.eventGrid;
			g.setSource(d.xwlMeta);
			g.store.sort('name', 'ASC');
			p.setSource(d.properties);
			p.store.sort('name', 'ASC');
			e.setSource(d.events);
			e.store.sort('name', 'ASC');
			g.customEditors = d.custGEditors;
			p.customEditors = d.custPEditors;
			e.customEditors = d.custEEditors;
		}

		function blockModify(a, b, s) {
			var c = s.customEditors[b.record.get('name')];
			if (c && c.field) {
				c.field.allowChange = false;
				if (c.field.blockPost)
					return false;
			}
			return true;
		}

		tab.add( {
			bindId : id,
			iconCls : iconCls,
			title : title,
			closable : true,
			layout : 'fit',
			xtype : 'tabpanel',
			deferredRender : false,
			listeners : {
				render : function(t) {
					page = t;
				},
				destroy : function(t) {
					if (controlTab.items.length == 0)
						Cm.setButtons(true);
				},
				beforeclose : function(t) {
					if (t.isModified) {
						tab.setActiveTab(t);
						Wb.choose(Wb.format(Str.saveConfirm, t.title
								.substring(1)), function(b) {
							if (b == 'yes') {
								Cm.saveModule(false, function() {
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
				iconCls : 'view_icon',
				xtype : 'propertygrid',
				nameColumnWidth : 170,
				source : {},
				listeners : {
					render : function(t) {
						page.generalGrid = t;
					},
					validateedit : function(a, b) {
						return blockModify(a, b, this);
					}
				}
			}, {
				title : 'Properties',
				iconCls : 'property_icon',
				xtype : 'propertygrid',
				nameColumnWidth : 170,
				source : {},
				listeners : {
					render : function(t) {
						page.propertyGrid = t;
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
		});
		tab.setActiveTab(page);
		Cm.setButtons(false);
		if (content)
			loadData(Ext.decode(content));
		else {
			Wb.request( {
				url : 'main?xwl=13L88K98GZO6&id=' + id,
				mask : page,
				failure : function(r) {
					page.close();
				},
				success : function(r) {
					loadData(Ext.decode(r.responseText));
				}
			});
		}
	},
	saveModule : function(isAll, func) {
		var hasData = false, para = {}, at = Cm.at(), tab = Wb
				.get('controlTab');
		tab.items.each(function(t) {
			if (t.isModified && (isAll || t == at)) {
				var obj = {};
				hasData = true;
				obj["xwlMeta"] = t.generalGrid.source;
				obj["properties"] = t.propertyGrid.source;
				obj["events"] = t.eventGrid.source;
				para['xwl_' + t.bindId] = Ext.encode(obj);
			}
		});
		if (hasData) {
			Wb.request( {
				url : 'main?xwl=13L88K98GZO7',
				params : para,
				mask : tab,
				success : function(resp) {
					tab.items.each(function(t) {
						if (t.isModified && (isAll || t == at))
							Wb.delModified(t);
						if (func)
							func();
					});
				}
			});
		}
	},
	getKey : function(edts, id) {
		for ( var n in edts)
			if (edts[n].field && edts[n].field.id == id)
				return n;
		return null;
	},
	monitorKey : function(o, e) {
		if (e.ctrlKey) {
			if (controlTab.items.length == 0)
				return;
			var k = e.getKey(), c = String.fromCharCode(k);
			if ((c == 'C' || c == 'V') && Wb.isEditor(e.getTarget()))
				return;
			switch (c) {
			case 'C':
				e.stopEvent();
				Cm.copyProperties();
				break;
			case 'V':
				e.stopEvent();
				Cm.pasteProperties();
				break;
			case 'S':
				e.stopEvent();
				if (e.shiftKey)
					Cm.saveModule(true);
				else
					Cm.saveModule(false);
				break;
			case 'N':
				e.stopEvent();
				Cm.insertControl(controlTree);
				break;
			case 'Q':
				e.stopEvent();
				Cm.insertProperty();
				break;
			}
		}
	},
	monitorElKey : function(e, o) {
		Cm.monitorKey(o, e);
	},
	monitorChange : function(o) {
		if (o.allowChange && o.isValid())
			Cm.setProperty(o);
	},
	monitorFocus : function(o) {
		o.allowChange = true;
	},
	populateDblClick : function(o) {
		o.isTriggerField = true;
		o.inputEl.dom.ondblclick = function() {
			o.onTriggerClick(o);
		}
	},
	setModified : function() {
		Wb.setModified(Cm.at());
	},
	setProperty : function(obj) {
		var page = Cm.at(), gg = page.generalGrid;
		var a = Cm.getKey(gg.customEditors, obj.id), v = obj.getValue();

		if (v == null || gg.source[a] === v)
			return;
		gg.source[a] = v;
		Cm.setModified();
	},
	getTypeData : function(isEvent) {
		var p = [ 'string', 'boolean', 'enum', 'enumMulti', 'bind', 'bindText',
				'bindMulti', 'text', 'express', 'object', 'date', 'iconClass',
				'url', 'urlList', 'sql', 'color', 'jndi', 'js', 'ss' ];
		var e = [ 'js', 'ss', 'object' ];
		if (isEvent)
			return e;
		else
			return p;
	},
	orgProperty : function(l, isNew) {
		var i;

		if (isNew)
			i = 1;
		else
			i = 0;
		var v = "{type:" + Ext.encode(l[i + 0]);
		if (!Ext.isEmpty(l[i + 1]))
			v += ",parameters:" + Ext.encode(l[i + 1]);
		if (!Ext.isEmpty(l[i + 2]))
			v += ",defaultValue: " + Ext.encode(l[i + 2]);
		v += "}";
		return v;
	},
	editProperty : function(sender) {
		var obj;
		if (sender.isTriggerField)
			obj = sender;
		else
			obj = this;
		var page = Cm.at(), pg = page.propertyGrid, eg = page.eventGrid, isEvent;

		function getProp() {
			var k = Cm.getKey(pg.customEditors, obj.id);
			if (k) {
				isEvent = false;
				pg.plugins[0].cancelEdit();
				return k;
			} else {
				isEvent = true;
				eg.plugins[0].cancelEdit();
				return Cm.getKey(eg.customEditors, obj.id);
			}
		}
		function setValue(l) {
			var v = Cm.orgProperty(l, false);
			var a = Cm.getKey(pg.customEditors, obj.id);
			if (a != null)
				pg.setProperty(a, v);
			else {
				a = Cm.getKey(eg.customEditors, obj.id);
				eg.setProperty(a, v);
			}
			Cm.setModified();
		}

		var name = getProp(), val = obj.getValue(), vo;
		if (Ext.isEmpty(val)) {
			vo = {
				type : 'string',
				parameters : '',
				defaultValue : ''
			};
			if (isEvent)
				vo.type = 'js';
		} else
			vo = Ext.decode(val);
		Wb.prompt('Edit ' + name, [ {
			text : 'type',
			value : vo.type,
			list : Cm.getTypeData(isEvent),
			allowBlank : false
		}, {
			text : 'parameters',
			value : Wb.optString(vo.parameters)
		}, {
			text : 'defaultValue',
			value : Wb.optString(vo.defaultValue)
		} ], setValue);
	},
	getTree : function(vt) {
		var tree = controlTree, processed = false;
		var n = tree.getRootNode(), type, name, index = 1, data = [];
		n.eachChild(function(t) {
			type = t.get('text');
			t.eachChild(function(l) {
				data.push( {
					META_TYPE : type,
					META_NAME : l.get('xwlMeta'),
					ORDER_INDEX : index
				});
				index++;
			});
			if (vt && type == vt.type) {
				data.push( {
					META_TYPE : vt.type,
					META_NAME : vt.name,
					ORDER_INDEX : index
				});
				processed = true;
				index++;
			}
		});
		if (!processed && vt) {
			data.push( {
				META_TYPE : vt.type,
				META_NAME : vt.name,
				ORDER_INDEX : index
			});
		}
		return data;
	},
	saveControlTree : function() {
		Wb.request( {
			url : 'main?xwl=13L88K98GZO8',
			mask : controlTree,
			params : {
				metaTree : Ext.encode(Cm.getTree())
			}
		});
	},
	deleteControl : function(tree) {
		var tree = controlTree, node = tree.selectControl;

		if (!node || node.getDepth() != 2) {
			Wb.warning(Str.selValid);
			return;
		}
		Wb.confirm(Wb.format(Str.delConfirm, node.get('text')), function() {
			var id = node.get('xwlMeta');
			Wb.request( {
				url : 'main?xwl=13L88K98GZOB',
				mask : tree,
				params : {
					META_NAME : id
				},
				success : function() {
					Wb.delSelNode(tree);
					var t = Cm.findTab(id);
					if (t) {
						t.isModified = false;
						t.close();
					}
				}
			});
		});
	},
	insertControl : function(tree) {
		var text, node = tree.selectControl;

		if (node) {
			if (node.getDepth() == 2)
				text = node.parentNode.get('text');
			else
				text = node.get('text');
		} else
			text = '';
		Wb.prompt('New Control', [ {
			text : 'Name',
			value : '',
			allowBlank : false
		}, {
			text : 'Category',
			value : text,
			allowBlank : false
		} ], function(l) {
			var r = tree.getRootNode();
			var p = r.findChild('text', l[1]);
			Wb.request( {
				url : 'main?xwl=13L88K98GZO9',
				mask : tree,
				params : {
					META_NAME : l[0],
					META_TYPE : l[1],
					metaTree : Ext.encode(Cm.getTree( {
						type : l[1],
						name : l[0]
					}))
				},
				success : function(x) {
					if (!p)
						p = r.appendChild( {
							text : l[1]
						});
					var n = p.appendChild( {
						text : l[0],
						iconCls : 'item_icon',
						xwlMeta : l[0],
						leaf : true
					});
					if (!p.isExpanded())
						p.expand();
					tree.view.select(n);
					Wb.closePrompt();
					Cm.createTab(l[0], l[0], 'item_icon', x.responseText);
				}
			});
			return false;
		});
	},
	eventInitType : 'js',
	propertyInitType : 'string',
	xwlDefine : [ 'xwlClass', 'xwlType', 'xwlXtype', 'xwlText', 'xwlIconCls',
			'xwlChildren', 'xwlParent', 'xwlCategory', 'xwlWidth', 'xwlHeight',
			'xwlMinWidth', 'xwlMaxWidth', 'xwlMinHeight', 'xwlMaxHeight' ],
	checkExists : function(name) {
		var t = controlTab.getActiveTab();
		return Wb.findRecord(t.generalGrid.store, 'name', name)
				|| Wb.findRecord(t.propertyGrid.store, 'name', name)
				|| Wb.findRecord(t.eventGrid.store, 'name', name);
	},
	insertProperty : function() {
		var t = controlTab.getActiveTab();
		if (!t)
			return;
		var g = t.getActiveTab();
		if (g == t.generalGrid) {
			Wb.prompt('New General Item', [ {
				text : 'Name',
				value : '',
				list : Cm.xwlDefine,
				allowBlank : false
			}, {
				text : 'Value',
				value : ''
			} ], function(l) {
				if (Cm.checkExists(l[0])) {
					Wb.warning(Wb.format(Str.alreadyExists, l[0]));
					return false;
				}
				g.setProperty(l[0], l[1], true);
				g.customEditors[l[0]] = new Ext.form.field.Trigger( {
					enableKeyEvents : true,
					hideTrigger : true,
					listeners : {
						keydown : Cm.monitorKey,
						change : Cm.monitorChange,
						focus : Cm.monitorFocus
					}
				});
				Cm.setModified();
			});
		} else {
			var title, isEvent = g == t.eventGrid, initType;
			if (isEvent) {
				title = 'Event';
				initType = Cm.eventInitType;
			} else {
				title = 'Property';
				initType = Cm.propertyInitType;
			}
			Wb.prompt('New ' + title, [ {
				text : 'name',
				value : '',
				allowBlank : false
			}, {
				text : 'type',
				value : initType,
				list : Cm.getTypeData(isEvent),
				allowBlank : false
			}, {
				text : 'parameters',
				value : ''
			}, {
				text : 'defaultValue',
				value : ''
			} ], function(l) {
				if (Cm.checkExists(l[0])) {
					Wb.warning(Wb.format(Str.alreadyExists, l[0]));
					return false;
				}
				var v = Cm.orgProperty(l, true);
				g.setProperty(l[0], v, true);
				g.customEditors[l[0]] = new Ext.form.field.Trigger( {
					enableKeyEvents : true,
					onTriggerClick : Cm.editProperty,
					listeners : {
						keydown : Cm.monitorKey,
						render : Cm.populateDblClick
					},
					editable : false,
					blockPost : true,
					triggerCls : 'ellipsis_icon'
				});
				if (isEvent)
					Cm.eventInitType = l[1];
				else
					Cm.propertyInitType = l[1];
				Cm.setModified();
			});
		}
	},
	copyProperties : function() {
		var t = Cm.at().getActiveTab();
		Cm.clipBoard = Ext.clone(t.source);
	},
	pasteProperties : function() {
		if (Cm.clipBoard == null)
			return;
		var b = false, n, t = Cm.at().getActiveTab(), d = Cm.clipBoard;
		for (n in d) {
			if (Cm.checkExists(n))
				continue;
			b = true;
			if (!Ext.isDefined(t.source.n)) {
				if (t == Cm.at().generalGrid) {
					t.customEditors[n] = new Ext.form.field.Trigger( {
						enableKeyEvents : true,
						hideTrigger : true,
						listeners : {
							keydown : Cm.monitorKey,
							change : Cm.monitorChange,
							focus : Cm.monitorFocus
						}
					});
				} else {
					t.customEditors[n] = new Ext.form.field.Trigger( {
						enableKeyEvents : true,
						onTriggerClick : Cm.editProperty,
						listeners : {
							keydown : Cm.monitorKey,
							render : Cm.populateDblClick
						},
						editable : false,
						blockPost : true,
						triggerCls : 'ellipsis_icon'
					});
				}
			}
			t.setProperty(n, d[n], true);
		}
		if (b)
			Cm.setModified();
	},
	deleteProperty : function() {
		var t = Cm.at(), g = t.getActiveTab(), p = g.getSelectionModel()
				.getCurrentPosition();
		if (p) {
			g.removeProperty(g.store.getAt(p.row).get('name'));
			Cm.setModified();
		}
	}
};