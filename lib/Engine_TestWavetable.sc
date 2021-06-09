// CroneEngine_TestWavetable
// dumbest possible test: a single, mono sinewave

// Inherit methods from CroneEngine
Engine_TestWavetable : CroneEngine {
    var maxNumVoices = 16;

    var waveTable;
    var wtLoaded = false;

    var amp = 0.3;
    var release = 0.5;
    var pw = 0.5;
    var cutoff = 1000;
    var gain = 2;
    var <synth;

    var <voices; // collection of voice nodes

	// Define a getter for the synth variable
	// var <synth;

	// Define a class method when an object is created
	*new { arg context, doneCallback;
		// Return the object from the superclass (CroneEngine) .new method
		^super.new(context, doneCallback);
	}
	// Rather than defining a SynthDef, use a shorthand to allocate a function and send it to the engine to play
	// Defined as an empty method in CroneEngine
	// https://github.com/monome/norns/blob/master/sc/core/CroneEngine.sc#L31
	alloc {

        voices = Dictionary.new;

        context.server.sync;
        
        SynthDef("wtTest", {
			arg out, gate = 1, freq = 440, startBuf=0, lastBuf=8, amp=amp, cutoff=cutoff, gain=gain, release=release;

            var snd, filt, lfo, env;

            lfo = SinOsc.kr(0.5, 0).range(startBuf + 0.1, lastBuf - 0.1);

            snd = VOsc3.ar(startBuf + lfo, freq+[0,1],freq+[0.37,1.1],freq+[0.43, -0.29], 0.3);

            env = EnvGen.ar(Env.adsr(0.1, 0.1, 0.8, release), gate, doneAction:2);

            snd = (env * snd) * amp;
			// filt = MoogFF.ar(snd, cutoff, gain) * amp;
			// env = Env.perc(level: amp, releaseTime: release).kr(2);

			Out.ar(out, snd);
		}).add;

        // context.server.sync;


        // synth = Synth.new(\ay1, [
		// 	\out, context.out_b.index
        //     ],
		// context.xg);

        context.server.sync;

        // synth.set(\volb, 4, \gate, 0);

        // load a wavetable
        this.addCommand("wt_load","s", { arg msg;

            ("Loading wavetable "++msg[1]).postln;

            if(wtLoaded,
                {
                    waveTable.do(_.free);
                    wtLoaded = false;
                });

            this.loadWaveform(context.server, msg[1], completionFunc: {
                arg wtArray;

                waveTable = wtArray;

                wtLoaded = true;

                waveTable.size.postln;

                ("Wavetable loading complete").postln;
            })
        });

        this.addCommand("note_on", "if", { arg msg;

            this.addVoice(msg[1], msg[2], true);
            // synth.set(\freqa, msg[1].midicps, \freqb, msg[1].midicps, \volb, 15, \gate, 1);
            // synth.set(\freqam, msg[1].midicps, \gate, 1);

            // if(synth != nil, {synth.free});

            // ("Note on").postln;

            // synth = Synth("wtTest", [\out, context.out_b, \freq, msg[2], \amp, 0.5, \startBuf, waveTable.first.bufnum]);

            // Synth("wtTest", [\out, context.out_b, \freq,val,\pw,pw,\amp,amp,\cutoff,cutoff,\gain,gain,\release,release], target:pg);
        });

        this.addCommand("note_off", "i", { arg msg;
            this.removeVoice(msg[1]);
            // synth.set(\amp, 0);

            // if(synth != nil, {synth.free});

            // synth.set(\gate, 0);
        });
	}


    // reads a wavetable from disk
    // returns an array of wavetable buffers in completionFunc
    // use with (Osc/Vosc)
    // wavSize must be a multiple of 2, eg.. 256, 512, 1024
    // the wav can have multiple waveforms, if more than 1 increase wavCount
    loadWaveform {
        arg server = nil, path = "waveform.wav", wavSize = 2048, wavCount = 0, completionFunc = {};

        var wavBuf, fArray, wtArray;
        var waveformRoutine;

        if (server.isNil, { server = Server.default; });

        waveformRoutine = Routine.new ({
            ("Reading buffer").postln;
            wavBuf = Buffer.read(server, path);

            server.sync;

            if((wavBuf.numFrames < (wavCount * wavSize)) || (wavCount < 1),
                {
                    wavCount = wavBuf.numFrames.div(wavSize);
                }
            );


            ("Num Frames = ").postln;
            wavBuf.numFrames.postln;

            wavBuf.loadToFloatArray(action: {arg array; fArray = array;});

            server.sync;
            ("Buffer loaded to array").postln;   

            wavBuf.free;

            server.sync;

            wtArray = Buffer.allocConsecutive(wavCount, server, wavSize * 2, 1);

            server.sync;

            ("Buffers alloc").postln;   

            wtArray.do({arg item, i;
                var sig, sigWt;

                ("Adding wt").postln;   


                sig = Signal.newClear(wavSize).waveFill({arg x, old, j; fArray[(wavSize * i) + j]});
                sigWt = sig.asWavetable;

                server.sync;

                item.loadCollection(sigWt);

                server.sync;
            });

            server.sync;


            ("wt Populated").postln;
            wtArray.first.bufnum.postln;
            wtArray.size.postln;

            // waveTable = wtArray;
            // wtLoaded = true;

            // ("Done").postln;

            // waveTable.size.postln;

            // Why is callback not working?
            completionFunc.value(wtArray);

        }).play;
    }

    addVoice { arg id, hz, map=true;
		var numVoices = voices.size;

		if(voices[id].notNil, {
			voices[id].set(\gate, 1);
			voices[id].set(\hz, hz);
		}, {
			if(numVoices < maxNumVoices, {
				voices.add(id -> Synth.new(\wtTest, [\out, context.out_b, \freq, hz, \amp, 0.5, \startBuf, waveTable.first.bufnum]));

				NodeWatcher.register(voices[id]);

				voices[id].onFree({
					voices.removeAt(id);
				});
			});
		});
	}

	removeVoice { arg id;
		if(true, {
			voices[id].set(\gate, 0);
		});
	}

	// define a function that is called when the synth is shut down
	free {
        if(wtLoaded,
                {
                    waveTable.do(_.free);
                    wtLoaded = false;
                });
		// synth.free;
	}
}