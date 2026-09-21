extends Node2D

const PlayerScript = preload("res://player.gd")
const SPEED: float = 230.0
const STATIONS: Array[Dictionary] = [
	{"id":"mentor", "name":"Chị Linh · Mentor", "point":Vector2(630, 710), "title":"Chào em, người đồng đội mới!", "text":"Chị là Linh, mentor của em. Hôm nay chưa cần bán hàng hay nhập liệu. Hãy đi một vòng, làm quen các khu vực và hỏi chị bất cứ lúc nào nhé!", "lesson":"Em vào vai nhân viên mới. Di chuyển bằng WASD, phím mũi tên hoặc bàn phím cảm ứng; đến gần điểm sáng rồi nhấn E để trò chuyện."},
	{"id":"checkout", "name":"Quầy thu ngân", "point":Vector2(456, 553), "title":"Một lời chào mở đầu trải nghiệm tốt", "text":"Đây là nơi đón khách và hoàn tất đơn hàng. Luôn chào khách, xác nhận sản phẩm và thông báo tổng tiền rõ ràng trước khi nhận tiền.", "lesson":"Ở chương bán hàng, em sẽ gặp khách, tư vấn, kiểm tra giỏ và xử lý hóa đơn trong các tình huống mô phỏng."},
	{"id":"shelves", "name":"Kệ hàng tiêu dùng", "point":Vector2(951, 297), "title":"Hiểu kệ hàng để giúp khách nhanh hơn", "text":"Các dãy kệ được chia theo nhóm sản phẩm. Khi khách hỏi, hãy lắng nghe nhu cầu rồi dẫn khách đến đúng khu vực, đừng chỉ tay và bỏ đi.", "lesson":"Quan sát nhãn giá, bao bì và vị trí sản phẩm. Những chương sau sẽ luyện tư vấn và sắp xếp hàng theo hạn dùng."},
	{"id":"chilled", "name":"Khu hàng lạnh", "point":Vector2(1630, 491), "title":"Hàng lạnh cần được chăm sóc riêng", "text":"Khu này dành cho sản phẩm cần bảo quản lạnh. Kiểm tra điều kiện bảo quản theo nhãn; phát hiện bao bì hở hay hàng quá hạn thì tách riêng và báo người phụ trách.", "lesson":"Không đưa hàng bất thường trở lại kệ bán. Chapter xử lý hàng hóa sẽ cho em thực hành nhận diện và báo cáo vấn đề."},
	{"id":"receiving", "name":"Khu nhận hàng", "point":Vector2(242, 521), "title":"Hàng mới bắt đầu hành trình ở đây", "text":"Hàng từ nhà cung cấp được kiểm số lượng, bao bì và hạn dùng trước khi nhập kho. Có chênh lệch thì ghi nhận và trao đổi, không tự đoán số lượng.", "lesson":"Đây chỉ là một phần nghiệp vụ cửa hàng, bên cạnh bán hàng, giao tiếp với khách và chăm sóc quầy kệ."},
	{"id":"carts", "name":"Giỏ hàng & lối vào", "point":Vector2(325, 720), "title":"Sẵn sàng đón vị khách đầu tiên", "text":"Giữ giỏ hàng gọn gàng và lối đi thông thoáng. Chủ động mời giỏ, hướng dẫn khách mới và chú ý những người cần hỗ trợ.", "lesson":"Em đã biết cách bắt đầu một ca làm việc: quan sát không gian, chào hỏi và hỗ trợ khách bằng thái độ thân thiện."}
]

var player: CharacterBody2D
var camera: Camera2D
var ui: CanvasLayer
var hint: Label
var progress: Label
var dialog: PanelContainer
var dialog_title: Label
var dialog_text: Label
var dialog_lesson: Label
var visited: Array[String] = []
var current: int = -1
var touch_direction: Vector2 = Vector2.ZERO
var finished: bool = false
var interact_button: Button
var compact_ui: bool = false
var dialog_next: Button
var quiz_choices: VBoxContainer
var quiz_index: int = -1
var quiz_answers: Dictionary = {}
const QUESTIONS: Array[Dictionary] = [
	{"key":"welcome", "question":"Khách cần tính tiền. Em hướng dẫn khách tới đâu?", "options":["Quầy thu ngân", "Khu nhận hàng", "Khu hàng lạnh"], "values":["checkout","receiving","chilled"], "answer":0, "hint":"Quầy thu ngân là nơi kiểm tra đơn, thông báo tổng tiền và hoàn tất lượt bán."},
	{"key":"sensitive", "question":"Khách muốn hủy hóa đơn hoặc trả hàng. Ai là người duyệt?", "options":["Nhân viên tự quyết", "Quản lý cửa hàng", "Nhờ kế toán duyệt thay"], "values":["sales","manager","accountant"], "answer":1, "hint":"Bình tĩnh tiếp nhận yêu cầu và chuyển quản lý cửa hàng. Thu ngân không tự hủy/hoàn tiền; kế toán không thay chốt duyệt vận hành."},
	{"key":"delivery", "question":"Nhà cung cấp vừa giao hàng. Hàng cần được đưa tới đâu trước?", "options":["Đưa ngay lên kệ bán", "Để ở lối vào", "Khu nhận hàng để kiểm tra"], "values":["shelves","carts","receiving"], "answer":2, "hint":"Hàng phải được kiểm nhận tại khu nhận hàng trước khi ghi nhận và đưa lên kệ. Giữ lối đi thông thoáng."}
]

func _ready() -> void:
	compact_ui = DisplayServer.window_get_size().x < 700 and OS.has_feature("web")
	if compact_ui:
		get_window().content_scale_size = Vector2i(640, 560)
	var backdrop := Sprite2D.new()
	backdrop.texture = preload("res://assets/store_map.png")
	backdrop.centered = false
	backdrop.show_behind_parent = true
	add_child(backdrop)
	build_collisions()
	player = CharacterBody2D.new()
	player.set_script(PlayerScript)
	player.position = Vector2(650, 750)
	player.z_index = 5
	var shape := CollisionShape2D.new()
	var circle := CircleShape2D.new()
	circle.radius = 10
	shape.shape = circle
	player.add_child(shape)
	add_child(player)
	var mentor := CharacterBody2D.new()
	mentor.set_script(PlayerScript)
	mentor.set("is_mentor", true)
	mentor.position = Vector2(625, 693)
	mentor.z_index = 4
	add_child(mentor)
	camera = Camera2D.new()
	camera.zoom = Vector2(1.05, 1.05)
	camera.position_smoothing_enabled = true
	camera.position_smoothing_speed = 7
	camera.limit_left = 50
	camera.limit_right = 1840
	camera.limit_top = 0
	camera.limit_bottom = 792
	player.add_child(camera)
	build_ui()
	bridge({"type":"ready"})

func obstacle(rect: Rect2) -> void:
	var body := StaticBody2D.new()
	var shape := CollisionShape2D.new()
	var box := RectangleShape2D.new()
	box.size = rect.size
	shape.shape = box
	body.position = rect.position + rect.size / 2
	body.add_child(shape)
	add_child(body)

func build_collisions() -> void:
	# Crop rectangles contain perspective tops and floor; feet collide only with furniture bases.
	var file := FileAccess.open("res://assets/layout.txt", FileAccess.READ)
	file.get_csv_line()
	while not file.eof_reached():
		var row: PackedStringArray = file.get_csv_line()
		if row.size() < 7:
			continue
		var x: float = float(row[2])
		var y: float = float(row[3])
		var w: float = float(row[4])
		var h: float = float(row[5])
		if row[0] == "checkout_barrier":
			continue
		obstacle(Rect2(x + 7, y + h * 0.35, w - 14, h * 0.60))
	obstacle(Rect2(50, 0, 24, 792))
	obstacle(Rect2(1810, 0, 100, 792))
	obstacle(Rect2(0, 776, 1900, 30))
	obstacle(Rect2(0, -20, 1900, 48))
	# The map's upper-left black area is outside the store.
	var wall := StaticBody2D.new()
	var polygon := CollisionPolygon2D.new()
	polygon.polygon = PackedVector2Array([Vector2(0,0),Vector2(797,0),Vector2(790,196),Vector2(75,335),Vector2(0,335)])
	wall.add_child(polygon)
	add_child(wall)

func label(text: String, size: int) -> Label:
	var node := Label.new()
	node.text = text
	node.add_theme_font_size_override("font_size", size)
	node.add_theme_color_override("font_color", Color("f6f0ff"))
	return node

func build_ui() -> void:
	ui = CanvasLayer.new()
	add_child(ui)
	var root := Control.new()
	root.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
	root.mouse_filter = Control.MOUSE_FILTER_IGNORE
	ui.add_child(root)
	var top := PanelContainer.new()
	top.position = Vector2(18, 16)
	top.custom_minimum_size = Vector2(370, 64)
	root.add_child(top)
	var column := VBoxContainer.new()
	top.add_child(column)
	column.add_child(label("  CHAPTER 0 · NGÀY ĐẦU TIÊN", 18))
	progress = label("  Khám phá cửa hàng  0 / 6", 16)
	column.add_child(progress)
	hint = label("Đến gần chị Linh ở điểm sáng để bắt đầu.", 17)
	hint.set_anchors_and_offsets_preset(Control.PRESET_CENTER_BOTTOM)
	hint.position = Vector2(290, -56)
	hint.add_theme_color_override("font_shadow_color",Color.BLACK)
	hint.add_theme_constant_override("shadow_outline_size", 5)
	root.add_child(hint)
	var controls := Control.new()
	controls.set_anchors_and_offsets_preset(Control.PRESET_BOTTOM_LEFT)
	controls.position = Vector2(18, -154)
	root.add_child(controls)
	var dirs: Array[Vector2] = [Vector2.UP, Vector2.LEFT, Vector2.DOWN, Vector2.RIGHT]
	var icons: Array[String] = ["W", "A", "S", "D"]
	var points: Array[Vector2] = [Vector2(56,0),Vector2(0,54),Vector2(56,54),Vector2(112,54)]
	for i in range(4):
		var b := Button.new()
		b.text = icons[i]
		b.position = points[i] * (1.3 if compact_ui else 1.0)
		b.size = Vector2(68,68) if compact_ui else Vector2(52,52)
		b.focus_mode = Control.FOCUS_NONE
		b.button_down.connect(func() -> void: touch_direction = dirs[i])
		b.button_up.connect(func() -> void: touch_direction = Vector2.ZERO)
		controls.add_child(b)
	interact_button = Button.new()
	interact_button.text = "E · Trò chuyện"
	interact_button.set_anchors_and_offsets_preset(Control.PRESET_BOTTOM_RIGHT)
	interact_button.position = Vector2(-208,-95)
	interact_button.size = Vector2(188,58)
	interact_button.pressed.connect(interact)
	root.add_child(interact_button)
	dialog = PanelContainer.new()
	dialog.set_anchors_and_offsets_preset(Control.PRESET_CENTER)
	dialog.offset_left = -310
	dialog.offset_right = 310
	dialog.offset_top = -176
	dialog.offset_bottom = 176
	var style := StyleBoxFlat.new()
	style.bg_color = Color("282039")
	style.border_color = Color("bb91ff")
	style.set_border_width_all(2)
	style.set_corner_radius_all(16)
	style.content_margin_left = 24
	style.content_margin_right = 24
	style.content_margin_top = 22
	style.content_margin_bottom = 22
	dialog.add_theme_stylebox_override("panel", style)
	root.add_child(dialog)
	var content := VBoxContainer.new()
	content.add_theme_constant_override("separation", 16)
	dialog.add_child(content)
	content.add_child(label("CHỊ LINH · MENTOR", 14))
	dialog_title = label("", 23)
	dialog_title.autowrap_mode = TextServer.AUTOWRAP_WORD_SMART
	content.add_child(dialog_title)
	dialog_text = label("", 19)
	dialog_text.autowrap_mode = TextServer.AUTOWRAP_WORD_SMART
	content.add_child(dialog_text)
	dialog_lesson = label("", 16)
	dialog_lesson.autowrap_mode = TextServer.AUTOWRAP_WORD_SMART
	dialog_lesson.add_theme_color_override("font_color", Color("cfb2fc"))
	content.add_child(dialog_lesson)
	quiz_choices = VBoxContainer.new()
	quiz_choices.add_theme_constant_override("separation", 8)
	content.add_child(quiz_choices)
	quiz_choices.hide()
	for choice in range(3):
		var button := Button.new()
		button.custom_minimum_size.y = 46 if not compact_ui else 56
		button.add_theme_font_size_override("font_size", 18 if not compact_ui else 22)
		button.pressed.connect(func() -> void: answer_quiz(choice))
		quiz_choices.add_child(button)
	var next := Button.new()
	dialog_next = next
	next.text = "Đã hiểu · Tiếp tục khám phá [E]"
	next.custom_minimum_size.y = 44
	next.pressed.connect(close_dialog)
	content.add_child(next)
	dialog.hide()
	if compact_ui:
		dialog_title.add_theme_font_size_override("font_size", 26)
		dialog_text.add_theme_font_size_override("font_size", 24)
		dialog_lesson.add_theme_font_size_override("font_size", 22)
		dialog.offset_left = -302
		dialog.offset_right = 302
		dialog.offset_top = -245
		dialog.offset_bottom = 245
		next.custom_minimum_size.y = 60
		next.add_theme_font_size_override("font_size", 22)
		interact_button.position = Vector2(-240,-110)
		interact_button.size = Vector2(218,72)
		interact_button.add_theme_font_size_override("font_size", 22)
		hint.hide()

func _physics_process(_delta: float) -> void:
	var direction := touch_direction
	if Input.is_physical_key_pressed(KEY_W) or Input.is_physical_key_pressed(KEY_UP): direction.y -= 1
	if Input.is_physical_key_pressed(KEY_S) or Input.is_physical_key_pressed(KEY_DOWN): direction.y += 1
	if Input.is_physical_key_pressed(KEY_A) or Input.is_physical_key_pressed(KEY_LEFT): direction.x -= 1
	if Input.is_physical_key_pressed(KEY_D) or Input.is_physical_key_pressed(KEY_RIGHT): direction.x += 1
	player.velocity = Vector2.ZERO if dialog.visible else direction.normalized() * SPEED
	player.set("moving", player.velocity.length() > 0)
	if direction.x != 0: player.set("facing", signf(direction.x))
	player.move_and_slide()
	current = -1
	var nearest: float = 78.0
	for i in range(STATIONS.size()):
		var distance: float = player.position.distance_to(STATIONS[i]["point"])
		if distance < nearest:
			nearest = distance
			current = i
	interact_button.disabled = current < 0 or dialog.visible
	hint.text = "E · " + str(STATIONS[current]["name"]) if current >= 0 else "Đi theo các điểm sáng · WASD / mũi tên"
	queue_redraw()

func _unhandled_key_input(event: InputEvent) -> void:
	if event is InputEventKey and event.pressed and not event.echo:
		if quiz_index >= 0 and quiz_index < QUESTIONS.size():
			if event.physical_keycode >= KEY_1 and event.physical_keycode <= KEY_3:
				answer_quiz(event.physical_keycode - KEY_1)
			return
		if event.physical_keycode == KEY_E or event.physical_keycode == KEY_ENTER:
			if dialog.visible: close_dialog()
			else: interact()
		if event.physical_keycode == KEY_ESCAPE and dialog.visible: close_dialog()

func _notification(what: int) -> void:
	if what == NOTIFICATION_WM_WINDOW_FOCUS_OUT:
		touch_direction = Vector2.ZERO

func interact() -> void:
	if current < 0 or dialog.visible: return
	var station: Dictionary = STATIONS[current]
	dialog_title.text = station["title"]
	dialog_text.text = station["text"]
	dialog_lesson.text = station["lesson"]
	dialog.set_meta("station", station["id"])
	dialog.show()
	touch_direction = Vector2.ZERO

func close_dialog() -> void:
	if quiz_index >= 0 and quiz_index < QUESTIONS.size(): return
	var station: String = str(dialog.get_meta("station", ""))
	if not station.is_empty() and not visited.has(station):
		visited.append(station)
		bridge({"type":"visit", "station":station})
	progress.text = "  Khám phá cửa hàng  %d / 6" % visited.size()
	dialog.hide()
	if visited.size() == STATIONS.size() and not finished:
		quiz_index = 0
		show_question()

func show_question() -> void:
	var question: Dictionary = QUESTIONS[quiz_index]
	dialog_title.text = "Cùng chị Linh xác nhận · %d / 3" % (quiz_index + 1)
	dialog_text.text = question["question"]
	dialog_lesson.text = "Chọn câu trả lời bên dưới hoặc nhấn 1, 2, 3. Em có thể thử lại."
	dialog.set_meta("station", "")
	dialog_next.hide()
	quiz_choices.show()
	for i in range(3):
		var button: Button = quiz_choices.get_child(i)
		button.text = "%d. %s" % [i + 1, question["options"][i]]
	dialog.show()

func answer_quiz(choice: int) -> void:
	if quiz_index < 0 or quiz_index >= QUESTIONS.size(): return
	var question: Dictionary = QUESTIONS[quiz_index]
	if choice != question["answer"]:
		dialog_lesson.text = "Chưa đúng nhé. " + str(question["hint"])
		return
	quiz_answers[question["key"]] = question["values"][choice]
	quiz_index += 1
	if quiz_index < QUESTIONS.size():
		show_question()
		return
	finished = true
	bridge({"type":"orientation", "answers":quiz_answers})
	bridge({"type":"complete"})
	quiz_choices.hide()
	dialog_next.show()
	dialog_next.text = "Đã hiểu · Tiếp tục khám phá [E]"
	dialog_title.text = "Em đã hoàn thành Chapter 0!"
	dialog_text.text = "Em đã khám phá 6 khu vực và biết ai là người hỗ trợ khi gặp yêu cầu nhạy cảm. Chị rất vui được đồng hành cùng em!"
	dialog_lesson.text = "Kết quả đang được gửi về hệ thống. Trở về màn chọn chương để kiểm tra trạng thái lưu; các chương tiếp theo đang được cập nhật."

func _draw() -> void:
	for i in range(STATIONS.size()):
		var station: Dictionary = STATIONS[i]
		var point: Vector2 = station["point"]
		var color := Color("74e7b0") if visited.has(station["id"]) else Color("dcadff")
		draw_circle(point, 27, Color(color,0.22))
		draw_arc(point, 27, 0, TAU, 40, color, 3)
		draw_circle(point + Vector2(0,-18), 9, color)
		draw_string(ThemeDB.fallback_font,point+Vector2(-4,-13),str(i+1),HORIZONTAL_ALIGNMENT_LEFT,-1,14,Color("282039"))
		if i == current:
			draw_arc(point, 35, 0, TAU, 40, Color.WHITE, 2)

func bridge(payload: Dictionary) -> void:
	if OS.has_feature("web"):
		payload["source"] = "simtim-godot"
		JavaScriptBridge.eval("window.parent.postMessage(%s, window.location.origin)" % JSON.stringify(payload))
