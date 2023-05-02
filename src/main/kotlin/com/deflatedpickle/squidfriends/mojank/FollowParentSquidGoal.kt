/* Copyright (c) 2023 DeflatedPickle under the MIT license */

package com.deflatedpickle.squidfriends.mojank

import com.deflatedpickle.squidfriends.api.IBreedable
import net.minecraft.entity.ai.goal.Goal
import net.minecraft.entity.passive.SquidEntity

open class FollowParentSquidGoal(
    private val animal: SquidEntity,
    private val speed: Double
) : Goal() {
    companion object {
        const val HORIZONTAL_SCAN_RANGE = 8
        const val VERTICAL_SCAN_RANGE = 4
        const val FOLLOW_MIN_DISTANCE = 3
    }

    private var parent: SquidEntity? = null
    private var delay = 0
    override fun canStart(): Boolean {
        return if ((animal as IBreedable).isAdult()) {
            false
        } else {
            val list = animal.world.getNonSpectatingEntities(animal.javaClass, animal.boundingBox.expand(8.0, 4.0, 8.0))
            var animalEntity: SquidEntity? = null
            var d = Double.MAX_VALUE
            for (animalEntity2 in list) {
                if (animalEntity2.isAdult()) {
                    val e = animal.squaredDistanceTo(animalEntity2)
                    if (!(e > d)) {
                        d = e
                        animalEntity = animalEntity2
                    }
                }
            }
            if (animalEntity == null) {
                false
            } else if (d < 9.0) {
                false
            } else {
                parent = animalEntity
                true
            }
        }
    }

    override fun shouldContinue(): Boolean {
        return if ((animal as IBreedable).isAdult()) {
            false
        } else if (!parent!!.isAlive) {
            false
        } else {
            val d = animal.squaredDistanceTo(parent)
            !(d < 9.0) && !(d > 256.0)
        }
    }

    override fun start() {
        delay = 0
    }

    override fun stop() {
        parent = null
    }

    override fun tick() {
        if (--delay <= 0) {
            delay = getTickCount(10)
            animal.navigation.startMovingTo(parent, speed)
        }
    }
}
