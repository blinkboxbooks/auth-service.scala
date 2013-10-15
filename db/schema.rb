# This file is auto-generated from the current state of the database. Instead
# of editing this file, please use the migrations feature of Active Record to
# incrementally modify your database, and then regenerate this schema definition.
#
# Note that this schema.rb definition is the authoritative source for your
# database schema. If you need to create the application database on another
# system, you should be using db:schema:load, not running all the migrations
# from scratch. The latter is a flawed and unsustainable approach (the more migrations
# you'll amass, the slower it'll run and the greater likelihood for issues).
#
# It's strongly recommended that you check this file into your version control system.

ActiveRecord::Schema.define(version: 7) do

  create_table "clients", force: true do |t|
    t.datetime "created_at"
    t.datetime "updated_at"
    t.integer  "user_id"
    t.string   "name",          limit: 50
    t.string   "model",         limit: 50
    t.string   "client_secret", limit: 50
    t.boolean  "deregistered",             default: false
    t.string   "brand",         limit: 50
    t.string   "os",            limit: 50
  end

  add_index "clients", ["user_id"], name: "index_clients_on_user_id", using: :btree

  create_table "password_reset_tokens", force: true do |t|
    t.datetime "created_at"
    t.datetime "updated_at"
    t.integer  "user_id",                               null: false
    t.string   "token",      limit: 50,                 null: false
    t.datetime "expires_at",                            null: false
    t.boolean  "revoked",               default: false, null: false
  end

  add_index "password_reset_tokens", ["token"], name: "index_password_reset_tokens_on_token", unique: true, using: :btree
  add_index "password_reset_tokens", ["user_id"], name: "index_password_reset_tokens_on_user_id", using: :btree

  create_table "refresh_tokens", force: true do |t|
    t.datetime "created_at"
    t.datetime "updated_at"
    t.integer  "user_id"
    t.integer  "client_id"
    t.string   "token",                         limit: 50
    t.datetime "expires_at"
    t.boolean  "revoked"
    t.datetime "elevation_expires_at"
    t.datetime "critical_elevation_expires_at"
  end

  add_index "refresh_tokens", ["token"], name: "index_refresh_tokens_on_token", unique: true, using: :btree

  create_table "users", force: true do |t|
    t.datetime "created_at"
    t.datetime "updated_at"
    t.string   "username",                       limit: 120
    t.string   "first_name",                     limit: 50
    t.string   "last_name",                      limit: 50
    t.string   "password_hash",                  limit: 128
    t.boolean  "allow_marketing_communications"
  end

  add_index "users", ["username"], name: "index_users_on_username", unique: true, using: :btree

end
