local music = require 'musicutil'

local Q7GridKeys = include("lib/Q7GridKeys")

local gridKeys = nil

local g = grid.connect()

local m = midi.connect()

local animateGrid = false

function init()
    gridKeys = Q7GridKeys.new(16,8) -- new Grid Keys

    gridKeys.note_on = grid_note_on
    gridKeys.note_off = grid_note_off
    gridKeys.key_pushed = grid_key_pushed


    gridKeys:resize_grid(1,1,16,8)
    gridKeys:change_scale(1, 1) -- set root note to C and scale to Major
    gridKeys.highlight_selected_notes = false
    gridKeys.layout_mode = 1 -- 1 = Chromatic, 2 = scale, 3 = drum

    if animateGrid then
        clock.run(grid_redraw_clock) -- start the grid redraw clock
    end

    grid_redraw()
end

function grid_redraw_clock() -- our grid redraw clock
    while true do -- while it's running...
        clock.sync(1/8)

        if gridKeys:animate() then
            grid_redraw()
        end
    end
end

function grid_key_pushed(gKeys, noteNum, vel)
end

function grid_note_on(gKeys, noteNum, vel)
    vel = vel or 100 
    print("Note On: " .. noteNum.. " " .. vel .. " " .. music.note_num_to_name(noteNum))
    -- engine.noteOn(noteNum, music.note_num_to_freq(noteNum), vel / 127)
end

function grid_note_off(gKeys, noteNum)
    -- m:note_off(noteNum, 0, gKeys.midi_channel)
    print("Note Off: " .. noteNum .. " " .. music.note_num_to_name(noteNum))
end

g.key = function(x,y,z)
    -- print("Key "..x.." "..y.." "..z)

    gridKeys:grid_key(x,y,z)

    grid_redraw()
end

function grid_redraw()
    g:all(0)
    gridKeys:draw_grid(g)
    g:refresh()
end

m.event = function(data)
    local d = midi.to_msg(data)

    -- tab.print(d)

    -- if d.type == "note_on" then
    --     transpose = d.note - 60
    -- end

    -- print(d)
end