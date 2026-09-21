extends CharacterBody2D

const EMPLOYEE: Texture2D = preload("res://assets/employee-walk-v2.png")
const MENTOR: Texture2D = preload("res://assets/mentor-walk-v2.png")
var moving: bool = false
var clock: float = 0.0
var facing: float = 1.0
var is_mentor: bool = false
var direction: Vector2 = Vector2.DOWN
var sheet: Texture2D
var frames: Array[Rect2] = []

func _ready() -> void:
	sheet = MENTOR if is_mentor else EMPLOYEE
	var source: Image = sheet.get_image()
	# Atlas windows match the sheets; trim transparent padding in memory.
	var columns := PackedInt32Array([125,340,540,740,950,1170,1440]) if is_mentor else PackedInt32Array([80,290,525,765,1005,1245,1500])
	var rows: Array[int] = [0,256,508,756,1024]
	for row in range(4):
		for column in range(6):
			var cell := Rect2i(columns[column],rows[row],columns[column+1]-columns[column],rows[row+1]-rows[row])
			var used: Rect2i = source.get_region(cell).get_used_rect()
			frames.append(Rect2(cell.position + used.position,used.size))
	queue_redraw()

func _process(delta: float) -> void:
	clock = clock + delta * 8.0 if moving else 0.0
	if velocity.length_squared() > 1:
		direction = velocity.normalized()
	queue_redraw()

func _draw() -> void:
	if frames.is_empty(): return
	draw_set_transform(Vector2.ZERO,0,Vector2(1,0.35))
	draw_circle(Vector2.ZERO,19,Color(0.12,0.06,0.18,0.22))
	draw_set_transform(Vector2.ZERO)
	var row: int = 0
	if absf(direction.x) > absf(direction.y): row = 2 if direction.x > 0 else 1
	elif direction.y < 0: row = 3
	var frame: int = int(clock) % 6 if moving else 0
	var region: Rect2 = frames[row * 6 + frame]
	var scale_factor: float = 82.0 / maxf(region.size.y,1.0)
	var size: Vector2 = region.size * scale_factor
	draw_texture_rect_region(sheet,Rect2(Vector2(-size.x/2,-size.y),size),region)
