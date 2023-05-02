/* Copyright (c) 2023 DeflatedPickle under the MIT license */

package com.deflatedpickle.squidfriends.api

import net.minecraft.entity.mob.WaterCreatureEntity
import net.minecraft.entity.passive.SquidEntity
import net.minecraft.server.world.ServerWorld

interface IBreedable {
    fun isAdult(): Boolean
    fun isInLove(): Boolean
    fun canBreedWith(other: WaterCreatureEntity): Boolean
    fun breed(world: ServerWorld, other: SquidEntity?)
}
