extends SceneTree

func _initialize() -> void:
	call_deferred("run_test")

func run_test() -> void:
	var store = load("res://main.tscn").instantiate()
	root.add_child(store)
	await physics_frame
	await physics_frame
	if store.player.frames.size() != 24:
		push_error("Player sprite atlas did not load 24 frames")
		quit(1)
		return
	var grid := AStarGrid2D.new()
	grid.region = Rect2i(0,0,93,40)
	grid.cell_size = Vector2(20,20)
	grid.diagonal_mode = AStarGrid2D.DIAGONAL_MODE_NEVER
	grid.update()
	var circle := CircleShape2D.new()
	circle.radius = 11.0
	var query := PhysicsShapeQueryParameters2D.new()
	query.shape = circle
	query.exclude = [store.player.get_rid()]
	for x in range(93):
		for y in range(40):
			query.transform = Transform2D(0,Vector2(x*20,y*20))
			grid.set_point_solid(Vector2i(x,y),not store.get_world_2d().direct_space_state.intersect_shape(query,1).is_empty())
	# Walk using the same movement/collision code as real keyboard and touch input.
	for station in store.STATIONS:
		var start := Vector2i((store.player.position / 20.0).round())
		var target := Vector2i((station["point"] / 20.0).round())
		var path: PackedVector2Array = grid.get_point_path(start,target)
		if path.is_empty():
			push_error("Unreachable station: " + station["id"])
			quit(1)
			return
		for point in path:
			var frames: int = 0
			while store.player.position.distance_to(point) > 4:
				store.touch_direction = store.player.position.direction_to(point)
				await physics_frame
				frames += 1
				if frames > 90:
					push_error("Movement blocked at " + str(point) + " for " + station["id"])
					quit(1)
					return
		store.touch_direction = Vector2.ZERO
		await physics_frame
		store.interact()
		if not store.dialog.visible or store.dialog.get_meta("station") != station["id"]:
			push_error("Wrong interaction at " + station["id"])
			quit(1)
			return
		store.close_dialog()
		print("PASS reached and visited: ",station["id"])
	if store.finished:
		push_error("Completion must require orientation answers")
		quit(1)
		return
	store.answer_quiz(1)
	if store.quiz_index != 0:
		push_error("Wrong answer advanced orientation")
		quit(1)
		return
	store.answer_quiz(0)
	store.answer_quiz(1)
	store.answer_quiz(2)
	if store.visited.size() != 6 or not store.finished:
		push_error("Chapter did not complete")
		quit(1)
		return
	print("PASS Chapter 0: 6 physical stations + wrong-answer feedback + 3 orientation answers; completion unlocked.")
	# Rebuild the same compact UI used by Web, including the longest wrong-answer hint.
	store.ui.free()
	store.compact_ui = true
	root.size = Vector2i(640,560)
	root.content_scale_size = Vector2i(640,560)
	store.build_ui()
	for question in range(3):
		store.quiz_index = question
		store.show_question()
		store.answer_quiz((int(store.QUESTIONS[question]["answer"]) + 1) % 3)
		await process_frame
		await process_frame
		var panel: Rect2 = store.dialog.get_global_rect()
		if panel.position.y < 0 or panel.end.y > 560 or panel.position.x < 0 or panel.end.x > 640:
			push_error("Compact orientation dialog overflows viewport: " + str(panel))
			quit(1)
			return
	print("PASS compact quiz: all 3 questions and wrong-answer feedback fit 640 x 560.")
	quit(0)
