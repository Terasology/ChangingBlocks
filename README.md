ChangingBlocks
==============

For supporting blocks that change through some sort of defined cycle, like crops, dust build-up, or just arbitrarily through configured options

Provide a list of block names and the game-time-in-milliseconds until the next block in the list is used.
When the last block is reached, either loop back to the first one, or send an OnBlockSequenceComplete event.

Example component to add to a prefab:

	"ChangingBlocks" : {
		"blockFamilyStages" : [
		  { "key": "Crops:Corn1", "value": 30000 },
			{ "key": "Crops:Corn2", "value": 30000 },
			{ "key": "Crops:Corn3", "value": 30000 },
			{ "key": "Crops:Corn4", "value": 30000 },
			{ "key": "Crops:Corn5", "value": 30000 },
			{ "key": "Crops:Corn6", "value": 30000 },
			{ "key": "Crops:Corn7", "value": 30000 }
    ],
		"loops" : false
	}

Example prefab to add to each block:

    "entity" : {
        "prefab" : "Crops:Corn",
        "keepActive" : true
    }
